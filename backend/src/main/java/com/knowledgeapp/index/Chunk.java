package com.knowledgeapp.index;

import java.util.List;

/** Chunk metadata per 04-knowledge-model.md: file, id, title, heading_path, tags. */
public record Chunk(
        String chunkId,
        String file,
        String docId,
        String title,
        List<String> headingPath,
        List<String> tags,
        String text) {

    /** Nearest heading, used as the citation heading. */
    public String heading() {
        return headingPath.isEmpty() ? title : headingPath.get(headingPath.size() - 1);
    }
}
