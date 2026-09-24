package com.auth_app_backend.dto.response;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Generic success response wrapper for all APIs.
 * @param <T> type of data payload
 */
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, OffsetDateTime.now(ZoneOffset.UTC));
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, OffsetDateTime.now(ZoneOffset.UTC));
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null, OffsetDateTime.now(ZoneOffset.UTC));
    }
}