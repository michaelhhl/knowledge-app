package com.knowledgeapp.knowledge;

import java.util.List;

/**
 * A parsed Markdown file. {@code path} is relative to the knowledge root and always uses '/'.
 * {@code body} excludes front matter.
 */
public record KnowledgeDocument(
        String path,
        String id,
        String title,
        List<String> tags,
        String updated,
        String status,
        String body) {

    public static final String STATUS_PUBLISHED = "published";
    public static final String STATUS_DRAFT = "draft";

    public boolean published() {
        return STATUS_PUBLISHED.equals(status);
    }

    public DocumentInfo info() {
        return new DocumentInfo(path, id, title, tags, updated);
    }
}
