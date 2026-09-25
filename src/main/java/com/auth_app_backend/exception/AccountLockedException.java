package com.auth_app_backend.exception;

import java.time.Instant;

public class AccountLockedException extends RuntimeException {

    private final Instant lockedUntil;

    public AccountLockedException(String message, Instant lockedUntil) {
        super(message);
        this.lockedUntil = lockedUntil;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }
}