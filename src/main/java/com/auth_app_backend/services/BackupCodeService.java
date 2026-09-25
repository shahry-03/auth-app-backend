package com.auth_app_backend.services;

import com.auth_app_backend.entity.User;

import java.util.List;

public interface BackupCodeService {

    /**
     * Generate N backup codes for a user.
     * Deletes any existing codes first.
     * Returns plaintext codes (only shown once to user).
     */
    List<String> generateCodes(User user, int count);

    /**
     * Verify a backup code and mark it as used.
     * Returns true if valid and not used.
     */
    boolean verifyAndConsume(User user, String code);

    /**
     * Count remaining unused backup codes.
     */
    long countRemaining(User user);

    /**
     * Delete all backup codes for a user.
     */
    void invalidateAll(User user);
}