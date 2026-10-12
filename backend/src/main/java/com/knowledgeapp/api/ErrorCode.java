package com.knowledgeapp.api;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    INVALID_PATH(HttpStatus.BAD_REQUEST),
    DOC_NOT_FOUND(HttpStatus.NOT_FOUND),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    LLM_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE),
    INDEX_NOT_READY(HttpStatus.SERVICE_UNAVAILABLE),
    LLM_ERROR(HttpStatus.BAD_GATEWAY),
    LLM_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY),
    INDEX_REBUILD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
