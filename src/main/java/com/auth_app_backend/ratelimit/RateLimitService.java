package com.auth_app_backend.ratelimit;

public interface RateLimitService {

    /**
     * Try to consume 1 token from the bucket.
     * @return true if allowed, false if limit exceeded
     */
    boolean tryConsume(String bucketName, String clientKey, RateLimit config);

    /**
     * Get remaining tokens for a client.
     */
    long getRemainingTokens(String bucketName, String clientKey, RateLimit config);

    /**
     * Reset bucket for a client (used in tests).
     */
    void reset(String bucketName, String clientKey);

    /**
     * Reset all buckets (used in tests).
     */
    void resetAll();
}