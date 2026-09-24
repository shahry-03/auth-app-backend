package com.auth_app_backend.helper;

import java.util.UUID;

/**
 * Utility methods for ID parsing and conversion.
 * All methods are static — this class should never be instantiated.
 */
public final class UserHelper {

    private UserHelper() {
        // utility class — no instances
    }

    /**
     * Parse a String into a UUID.
     *
     * @param id the string to parse
     * @return UUID
     * @throws IllegalArgumentException if id is null, empty, or invalid UUID format
     */
    public static UUID parseId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid UUID format: " + id);
        }
    }

    /**
     * Safely parse a String into a UUID — returns null if invalid.
     */
    public static UUID parseIdOrNull(String id) {
        if (id == null || id.isBlank()) return null;
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}