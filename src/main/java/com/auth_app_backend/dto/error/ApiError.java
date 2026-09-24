package com.auth_app_backend.dto.error;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Standard error response for all APIs.
 * @param fieldErrors validation errors — field name → error message
 */
public record ApiError(
    int status,
    String error,
    String message,
    String path,
    OffsetDateTime timestamp,
    Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path,
            OffsetDateTime.now(ZoneOffset.UTC), null);
    }

    public static ApiError ofValidation(int status, String message, String path,
                                        Map<String, String> fieldErrors) {
        return new ApiError(status, "Validation Failed", message, path,
            OffsetDateTime.now(ZoneOffset.UTC), fieldErrors);
    }
}