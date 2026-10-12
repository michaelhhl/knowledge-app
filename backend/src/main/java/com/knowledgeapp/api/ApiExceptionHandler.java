package com.knowledgeapp.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        if (ex.code().status().is5xxServerError()) {
            log.warn("API error {}: {}", ex.code(), ex.getMessage());
        }
        return respond(ex.code(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .orElse("请求参数无效。");
        return respond(ErrorCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        return respond(ErrorCode.VALIDATION_ERROR, "请求格式无效。");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex) {
        return respond(ErrorCode.NOT_FOUND, "资源不存在。");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "不支持的请求方法。"));
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ErrorResponse> handleSpring(ErrorResponseException ex) {
        ErrorCode code = ex.getStatusCode().is4xxClientError() ? ErrorCode.VALIDATION_ERROR : ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(ex.getStatusCode()).body(ErrorResponse.of(code, "请求无法处理。"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return respond(ErrorCode.INTERNAL_ERROR, "服务内部错误。");
    }

    private static ResponseEntity<ErrorResponse> respond(ErrorCode code, String message) {
        return ResponseEntity.status(code.status()).body(ErrorResponse.of(code, message));
    }
}
