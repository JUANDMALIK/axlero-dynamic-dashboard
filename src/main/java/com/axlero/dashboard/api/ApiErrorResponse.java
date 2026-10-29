package com.axlero.dashboard.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Standard API error response")
public record ApiErrorResponse(
        @Schema(description = "Time the error occurred")
        Instant timestamp,
        @Schema(description = "HTTP status code", example = "400")
        int status,
        @Schema(description = "HTTP status phrase", example = "Bad Request")
        String error,
        @Schema(description = "Human-readable error message")
        String message,
        @Schema(description = "Request path that caused the error")
        String path,
        @Schema(description = "Field-specific validation errors; empty when not applicable")
        Map<String, String> validationErrors) {
}
