package com.auth_app_backend.helper;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Extracts request metadata (IP, User-Agent) safely.
 * Handles proxy headers (X-Forwarded-For, X-Real-IP).
 */
public final class RequestMetadataExtractor {

    private RequestMetadataExtractor() {
        // utility class
    }

    /**
     * Extract client IP address.
     * Priority: X-Forwarded-For → X-Real-IP → getRemoteAddr()
     */
    public static String extractIp(HttpServletRequest request) {
        if (request == null) return null;

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    /**
     * Extract User-Agent header, truncated to fit DB column.
     */
    public static String extractUserAgent(HttpServletRequest request) {
        if (request == null) return null;

        String ua = request.getHeader("User-Agent");
        if (ua == null) return null;

        // Truncate to 500 chars (DB column limit)
        return ua.length() > 500 ? ua.substring(0, 500) : ua;
    }
}