package com.knowledgeapp.knowledge;

import com.knowledgeapp.api.ApiException;
import com.knowledgeapp.api.ErrorCode;
import com.knowledgeapp.config.AppPaths;
import com.knowledgeapp.index.IndexManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/** FR-05: table of contents and single-document reads, limited to published, indexed files. */
@Service
public class KnowledgeService {

    private static final String INDEX_FILE = "_index.md";

    private final AppPaths paths;
    private final IndexManager indexManager;

    public KnowledgeService(AppPaths paths, IndexManager indexManager) {
        this.paths = paths;
        this.indexManager = indexManager;
    }

    public record TocItem(String title, String path) {
    }

    public record Toc(boolean empty, List<TocItem> items, List<TocItem> others) {
    }

    public record DocumentView(
            String path, String id, String title, List<String> tags, String updated, String markdown) {
    }

    public List<DocumentInfo> documents() {
        return indexManager.documents();
    }

    public Toc toc() {
        List<DocumentInfo> docs = indexManager.documents();
        List<TocItem> items = new ArrayList<>();
        Set<String> listed = new LinkedHashSet<>();
        for (TocParser.TocLink link : TocParser.parse(readIndexFile())) {
            String normalized;
            try {
                normalized = KnowledgePaths.normalizeRelative(link.target());
            } catch (ApiException ex) {
                continue;
            }
            // Drafts and unpublished files are not listed (07-ui-ux.md).
            if (indexManager.findDocument(normalized).isPresent() && listed.add(normalized)) {
                items.add(new TocItem(link.title(), normalized));
            }
        }
        List<TocItem> others = docs.stream()
                .filter(d -> !listed.contains(d.path()))
                .map(d -> new TocItem(d.title(), d.path()))
                .toList();
        return new Toc(docs.isEmpty(), items, others);
    }

    public DocumentView read(String requestedPath) {
        String normalized = KnowledgePaths.normalizeRelative(requestedPath);
        if (KnowledgeScanner.isMetaFile(normalized) || indexManager.findDocument(normalized).isEmpty()) {
            throw notFound();
        }
        Path file = KnowledgePaths.resolveExisting(paths.knowledgeRoot(), normalized);
        KnowledgeDocument doc;
        try {
            doc = MarkdownParser.parse(normalized, Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw notFound();
        }
        if (!doc.published()) {
            throw notFound();
        }
        return new DocumentView(doc.path(), doc.id(), doc.title(), doc.tags(), doc.updated(), doc.body());
    }

    private String readIndexFile() {
        Path index = paths.knowledgeRoot().resolve(INDEX_FILE);
        try {
            return Files.isRegularFile(index) ? Files.readString(index, StandardCharsets.UTF_8) : "";
        } catch (IOException ex) {
            return "";
        }
    }

    private static ApiException notFound() {
        return new ApiException(ErrorCode.DOC_NOT_FOUND, "文档不存在或未发布。");
    }
}
