package com.knowledgeapp.index;

import com.knowledgeapp.knowledge.DocumentInfo;
import java.time.Instant;
import java.util.List;

/** Stored next to the Lucene files of one index generation. */
public record IndexMetadata(
        int schemaVersion,
        Instant builtAt,
        SourceManifest manifest,
        List<DocumentInfo> documents,
        int chunkCount) {
}
