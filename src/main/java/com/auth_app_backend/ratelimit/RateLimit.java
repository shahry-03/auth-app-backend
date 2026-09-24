package com.auth_app_backend.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rate limiting annotation — applies Token Bucket algorithm per client IP.
 *
 * Example:
 *   @RateLimit(name = "login", capacity = 5, refillTokens = 5, refillPeriod = 15)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Unique name for the bucket (typically the endpoint name).
     */
    String name();

    /**
     * Maximum number of tokens (requests) in the bucket.
     */
    long capacity();

    /**
     * Tokens added on each refill.
     */
    long refillTokens() default 0;   // 0 = use capacity

    /**
     * Refill period in minutes.
     */
    long refillPeriod() default 1;

    /**
     * Optional: time unit for refill period (default: minutes).
     */
    RefillUnit refillUnit() default RefillUnit.MINUTES;

    enum RefillUnit {
        SECONDS, MINUTES, HOURS
    }
}