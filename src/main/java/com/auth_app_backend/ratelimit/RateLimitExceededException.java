package com.auth_app_backend.exception;

import com.auth_app_backend.ratelimit.RateLimit;

public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public RateLimitExceededException(String message,
                                      long refillPeriod,
                                      RateLimit.RefillUnit unit) {
        super(message);
        this.retryAfterSeconds = toSeconds(refillPeriod, unit);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    private static long toSeconds(long period, RateLimit.RefillUnit unit) {
        return switch (unit) {
            case SECONDS -> period;
            case MINUTES -> period * 60;
            case HOURS -> period * 3600;
        };
    }
}