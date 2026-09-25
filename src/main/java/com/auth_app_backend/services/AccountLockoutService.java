package com.auth_app_backend.services;

import com.auth_app_backend.entity.User;

public interface AccountLockoutService {

    /**
     * Check if account is locked. Throws AccountLockedException if locked.
     */
    void checkAccountLocked(User user);

    /**
     * Record a failed login attempt.
     * If threshold exceeded, locks the account.
     * Returns true if account was just locked.
     */
    boolean recordFailedAttempt(User user);

    /**
     * Reset failed attempts counter (on successful login).
     */
    void resetFailedAttempts(User user);

    /**
     * Manually unlock an account (admin action).
     */
    void unlockAccount(User user);
}