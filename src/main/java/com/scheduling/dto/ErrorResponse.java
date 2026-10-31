package com.scheduling.dto;

import java.time.Instant;

/**
 * Structured error response body returned by all error-producing endpoints.
 * Provides consistent shape for API consumers to parse.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(Instant.now(), status, error, message, path);
    }
}
