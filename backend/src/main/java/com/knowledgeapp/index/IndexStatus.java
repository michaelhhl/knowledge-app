package com.knowledgeapp.index;

import java.time.Instant;

public record IndexStatus(
        State state,
        Instant builtAt,
        int documentCount,
        int chunkCount,
        boolean stale,
        String lastError) {

    public enum State {
        NOT_READY,
        BUILDING,
        READY,
        FAILED
    }
}
