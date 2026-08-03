package com.financialhub.interfaces.rest.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String path,
        Map<String, String> fields
) {
    public ErrorResponse(String code, String message, Instant timestamp, String path) {
        this(code, message, timestamp, path, null);
    }
}
