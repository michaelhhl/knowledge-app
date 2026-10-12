package com.knowledgeapp.index;

import com.knowledgeapp.knowledge.KnowledgeScanner.SourceFile;
import java.util.List;

/** File list (relative path, size, mtime) used to detect knowledge changes. */
public record SourceManifest(List<Entry> entries) {

    public record Entry(String path, long size, long lastModified) {
    }

    public static SourceManifest of(List<SourceFile> sources) {
        return new SourceManifest(sources.stream()
                .map(s -> new Entry(s.relativePath(), s.size(), s.lastModified()))
                .toList());
    }
}
