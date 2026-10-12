package com.knowledgeapp.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgeapp.api.ApiException;
import com.knowledgeapp.api.ErrorCode;
import com.knowledgeapp.config.AppPaths;
import com.knowledgeapp.knowledge.DocumentInfo;
import com.knowledgeapp.knowledge.KnowledgeDocument;
import com.knowledgeapp.knowledge.KnowledgeScanner;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Lucene BM25 index (06-architecture.md). Each rebuild writes a new generation directory and
 * switches the CURRENT pointer atomically, so a failed rebuild keeps the previous index.
 */
@Component
public class IndexManager implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IndexManager.class);
    private static final String CURRENT_FILE = "CURRENT";
    private static final String METADATA_FILE = "metadata.json";
    private static final String GEN_PREFIX = "gen-";
    private static final String PATH_SEPARATOR = "\u001F";
    /** Bump when analyzer, stopwords or stored fields change so existing indexes are rebuilt. */
    static final int SCHEMA_VERSION = 1;

    private static final String F_CHUNK_ID = "chunkId";
    private static final String F_FILE = "file";
    private static final String F_DOC_ID = "docId";
    private static final String F_TITLE = "title";
    private static final String F_HEADINGS = "headingPath";
    private static final String F_TAGS = "tags";
    private static final String F_TEXT = "text";
    private static final String F_CONTENT = "content";

    private final AppPaths paths;
    private final ObjectMapper objectMapper;
    private final Analyzer analyzer = createAnalyzer();
    private final AtomicReference<Snapshot> current = new AtomicReference<>();
    private final Object rebuildLock = new Object();

    private volatile boolean building;
    private volatile String lastError;

    public IndexManager(AppPaths paths, ObjectMapper objectMapper) {
        this.paths = paths;
        this.objectMapper = objectMapper;
    }

    private record Snapshot(String generation, Directory directory, DirectoryReader reader, IndexMetadata metadata) {
    }

    @Override
    public void run(ApplicationArguments args) {
        initialize();
    }

    /** FR-06: load the existing index when sources are unchanged, otherwise rebuild. */
    public void initialize() {
        try {
            Optional<String> generation = readCurrentPointer();
            if (generation.isPresent()) {
                Snapshot loaded = openGeneration(generation.get());
                SourceManifest now = SourceManifest.of(KnowledgeScanner.listSources(paths.knowledgeRoot()));
                if (loaded.metadata().schemaVersion() == SCHEMA_VERSION && now.equals(loaded.metadata().manifest())) {
                    swap(loaded);
                    deleteStaleGenerations(loaded.generation());
                    log.info("Loaded index {} ({} chunks)", loaded.generation(), loaded.metadata().chunkCount());
                    return;
                }
                closeQuietly(loaded);
                log.info("Knowledge sources changed since last index; rebuilding");
            }
        } catch (IOException | RuntimeException ex) {
            log.info("Existing index unusable ({}); rebuilding", ex.getMessage());
        }
        try {
            rebuild();
        } catch (ApiException ex) {
            log.warn("Initial index build failed: {}", ex.getMessage());
        }
    }

    public IndexStatus rebuild() {
        synchronized (rebuildLock) {
            building = true;
            String generation = GEN_PREFIX + System.currentTimeMillis();
            Path genDir = paths.indexDir().resolve(generation);
            try {
                List<KnowledgeScanner.SourceFile> sources = KnowledgeScanner.listSources(paths.knowledgeRoot());
                List<KnowledgeDocument> docs = KnowledgeScanner.scanDocuments(paths.knowledgeRoot()).stream()
                        .filter(KnowledgeDocument::published)
                        .toList();
                List<Chunk> chunks = new ArrayList<>();
                docs.forEach(d -> chunks.addAll(Chunker.chunk(d)));

                Files.createDirectories(genDir);
                try (Directory dir = FSDirectory.open(genDir);
                     IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(analyzer))) {
                    for (Chunk chunk : chunks) {
                        writer.addDocument(toLucene(chunk));
                    }
                    writer.commit();
                }
                IndexMetadata metadata = new IndexMetadata(
                        SCHEMA_VERSION,
                        Instant.now(),
                        SourceManifest.of(sources),
                        docs.stream().map(KnowledgeDocument::info).toList(),
                        chunks.size());
                Files.writeString(genDir.resolve(METADATA_FILE), objectMapper.writeValueAsString(metadata),
                        StandardCharsets.UTF_8);

                Snapshot snapshot = openGeneration(generation);
                writeCurrentPointer(generation);
                swap(snapshot);
                deleteStaleGenerations(generation);
                lastError = null;
                building = false;
                log.info("Built index {} ({} documents, {} chunks)", generation, docs.size(), chunks.size());
                return status();
            } catch (IOException | RuntimeException ex) {
                deleteRecursively(genDir);
                lastError = "索引重建失败：" + ex.getMessage();
                log.warn("Index rebuild failed", ex);
                throw new ApiException(ErrorCode.INDEX_REBUILD_FAILED, lastError, ex);
            } finally {
                building = false;
            }
        }
    }

    public IndexStatus status() {
        Snapshot snapshot = current.get();
        if (snapshot == null) {
            IndexStatus.State state = building ? IndexStatus.State.BUILDING
                    : lastError != null ? IndexStatus.State.FAILED : IndexStatus.State.NOT_READY;
            return new IndexStatus(state, null, 0, 0, false, lastError);
        }
        IndexMetadata meta = snapshot.metadata();
        return new IndexStatus(
                building ? IndexStatus.State.BUILDING : IndexStatus.State.READY,
                meta.builtAt(),
                meta.documents().size(),
                meta.chunkCount(),
                isStale(meta),
                lastError);
    }

    public boolean isReady() {
        return current.get() != null;
    }

    /** Published documents registered in the current index. */
    public List<DocumentInfo> documents() {
        return requireSnapshot().metadata().documents();
    }

    public Optional<DocumentInfo> findDocument(String relativePath) {
        return documents().stream().filter(d -> d.path().equals(relativePath)).findFirst();
    }

    public List<SearchHit> search(String queryText, int limit) {
        Snapshot snapshot = requireSnapshot();
        DirectoryReader reader = snapshot.reader();
        if (!reader.tryIncRef()) {
            snapshot = requireSnapshot();
            reader = snapshot.reader();
            if (!reader.tryIncRef()) {
                throw new ApiException(ErrorCode.INDEX_NOT_READY, "索引正在切换，请稍后重试。");
            }
        }
        try {
            Query query = parseQuery(queryText);
            if (query == null) {
                return List.of();
            }
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs top = searcher.search(query, Math.max(1, limit));
            List<SearchHit> hits = new ArrayList<>();
            for (ScoreDoc sd : top.scoreDocs) {
                hits.add(new SearchHit(fromLucene(searcher.storedFields().document(sd.doc)), sd.score));
            }
            return hits;
        } catch (IOException ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "检索失败。", ex);
        } finally {
            try {
                reader.decRef();
            } catch (IOException ignored) {
                // reader already closed
            }
        }
    }

    @PreDestroy
    public void close() {
        Snapshot snapshot = current.getAndSet(null);
        if (snapshot != null) {
            closeQuietly(snapshot);
        }
    }

    private Snapshot requireSnapshot() {
        Snapshot snapshot = current.get();
        if (snapshot == null) {
            String message = lastError != null ? lastError : "索引尚未建立，请先重建索引。";
            throw new ApiException(ErrorCode.INDEX_NOT_READY, message);
        }
        return snapshot;
    }

    private Query parseQuery(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            QueryParser parser = new QueryParser(F_CONTENT, analyzer);
            Query query = parser.parse(QueryParser.escape(text.strip()));
            if (query instanceof BooleanQuery bq && bq.clauses().isEmpty()) {
                return null;
            }
            return query;
        } catch (ParseException | IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isStale(IndexMetadata meta) {
        try {
            return !SourceManifest.of(KnowledgeScanner.listSources(paths.knowledgeRoot())).equals(meta.manifest());
        } catch (IOException ex) {
            return true;
        }
    }

    private void swap(Snapshot next) {
        Snapshot previous = current.getAndSet(next);
        if (previous != null) {
            closeQuietly(previous);
        }
    }

    private Snapshot openGeneration(String generation) throws IOException {
        Path genDir = paths.indexDir().resolve(generation);
        IndexMetadata metadata = objectMapper.readValue(
                Files.readString(genDir.resolve(METADATA_FILE), StandardCharsets.UTF_8), IndexMetadata.class);
        Directory directory = FSDirectory.open(genDir);
        try {
            return new Snapshot(generation, directory, DirectoryReader.open(directory), metadata);
        } catch (IOException | RuntimeException ex) {
            directory.close();
            throw ex;
        }
    }

    private Optional<String> readCurrentPointer() throws IOException {
        Path pointer = paths.indexDir().resolve(CURRENT_FILE);
        if (!Files.isRegularFile(pointer)) {
            return Optional.empty();
        }
        String generation = Files.readString(pointer, StandardCharsets.UTF_8).strip();
        if (!generation.startsWith(GEN_PREFIX) || generation.contains("/") || generation.contains("\\")) {
            return Optional.empty();
        }
        return Optional.of(generation);
    }

    private void writeCurrentPointer(String generation) throws IOException {
        Path pointer = paths.indexDir().resolve(CURRENT_FILE);
        Path temp = paths.indexDir().resolve(CURRENT_FILE + ".tmp");
        Files.writeString(temp, generation, StandardCharsets.UTF_8);
        try {
            Files.move(temp, pointer, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, pointer, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteStaleGenerations(String keep) {
        try (Stream<Path> children = Files.list(paths.indexDir())) {
            children.filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith(GEN_PREFIX))
                    .filter(p -> !p.getFileName().toString().equals(keep))
                    .forEach(IndexManager::deleteRecursively);
        } catch (IOException ex) {
            log.debug("Could not list index directory: {}", ex.getMessage());
        }
    }

    private static void deleteRecursively(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // memory-mapped files on Windows may stay locked until GC; retried on next rebuild
                }
            });
        } catch (IOException ignored) {
            // best effort
        }
    }

    private static void closeQuietly(Snapshot snapshot) {
        try {
            snapshot.reader().decRef();
        } catch (IOException | RuntimeException ignored) {
            // already closed
        }
        try {
            snapshot.directory().close();
        } catch (IOException ignored) {
            // already closed
        }
    }

    private static Analyzer createAnalyzer() {
        CharArraySet stopWords = CharArraySet.copy(SmartChineseAnalyzer.getDefaultStopSet());
        try (InputStream in = IndexManager.class.getResourceAsStream("/stopwords-zh.txt")) {
            if (in != null) {
                new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                        .map(String::strip)
                        .filter(s -> !s.isEmpty())
                        .forEach(stopWords::add);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return new SmartChineseAnalyzer(stopWords);
    }

    private static Document toLucene(Chunk chunk) {
        Document doc = new Document();
        doc.add(new StringField(F_CHUNK_ID, chunk.chunkId(), Field.Store.YES));
        doc.add(new StringField(F_FILE, chunk.file(), Field.Store.YES));
        doc.add(new StoredField(F_DOC_ID, chunk.docId()));
        doc.add(new StoredField(F_TITLE, chunk.title()));
        doc.add(new StoredField(F_HEADINGS, String.join(PATH_SEPARATOR, chunk.headingPath())));
        doc.add(new StoredField(F_TAGS, String.join(PATH_SEPARATOR, chunk.tags())));
        doc.add(new StoredField(F_TEXT, chunk.text()));
        String searchable = String.join("\n",
                String.join(" ", chunk.headingPath()),
                String.join(" ", chunk.tags()),
                chunk.text());
        doc.add(new TextField(F_CONTENT, searchable, Field.Store.NO));
        return doc;
    }

    private static Chunk fromLucene(Document doc) {
        return new Chunk(
                doc.get(F_CHUNK_ID),
                doc.get(F_FILE),
                doc.get(F_DOC_ID),
                doc.get(F_TITLE),
                splitStored(doc.get(F_HEADINGS)),
                splitStored(doc.get(F_TAGS)),
                doc.get(F_TEXT));
    }

    private static List<String> splitStored(String value) {
        if (value == null || value.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(value.split(PATH_SEPARATOR));
    }
}
