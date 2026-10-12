package com.knowledgeapp.knowledge;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Walks the knowledge root. Meta files (leading '_') and hidden paths are skipped. */
public final class KnowledgeScanner {

    /** Root plus up to two levels of category folders (04-knowledge-model.md). */
    private static final int MAX_DEPTH = 3;

    private KnowledgeScanner() {
    }

    public record SourceFile(String relativePath, Path file, long size, long lastModified) {
    }

    /** All Markdown files that influence the index, including meta and draft files. */
    public static List<SourceFile> listSources(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IOException("Knowledge root does not exist: " + root.getFileName());
        }
        Path realRoot = root.toRealPath();
        List<SourceFile> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(realRoot, MAX_DEPTH)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".md"))
                    .filter(p -> !isHidden(realRoot.relativize(p)))
                    .forEach(p -> result.add(toSource(realRoot, p)));
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
        result.sort(Comparator.comparing(SourceFile::relativePath));
        return result;
    }

    /** Parses every non-meta Markdown file. Draft filtering is left to callers. */
    public static List<KnowledgeDocument> scanDocuments(Path root) throws IOException {
        Path realRoot = root.toRealPath();
        List<KnowledgeDocument> docs = new ArrayList<>();
        for (SourceFile source : listSources(root)) {
            if (isMetaFile(source.relativePath())) {
                continue;
            }
            Path real = source.file().toRealPath();
            if (!real.startsWith(realRoot)) {
                continue;
            }
            docs.add(MarkdownParser.parse(source.relativePath(), Files.readString(real, StandardCharsets.UTF_8)));
        }
        return docs;
    }

    public static boolean isMetaFile(String relativePath) {
        String name = relativePath.substring(relativePath.lastIndexOf('/') + 1);
        return name.startsWith("_");
    }

    private static boolean isHidden(Path relative) {
        for (Path part : relative) {
            if (part.toString().startsWith(".")) {
                return true;
            }
        }
        return false;
    }

    private static SourceFile toSource(Path root, Path file) {
        try {
            String relative = root.relativize(file).toString().replace('\\', '/');
            return new SourceFile(relative, file, Files.size(file), Files.getLastModifiedTime(file).toMillis());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
