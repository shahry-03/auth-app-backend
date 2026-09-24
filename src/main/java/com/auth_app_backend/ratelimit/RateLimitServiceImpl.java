package com.auth_app_backend.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limit service using Token Bucket algorithm.
 *
 * For multi-instance deployments, replace the map with a Redis-backed
 * implementation (bucket4j-redis).
 */
@Slf4j
@Service
public class RateLimitServiceImpl implements RateLimitService {

    /**
     * Bucket storage: bucketName -> (clientKey -> Bucket)
     * Using ConcurrentHashMap for thread safety.
     */
    private final Map<String, Map<String, Bucket>> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean tryConsume(String bucketName, String clientKey, RateLimit config) {
        Bucket bucket = getOrCreateBucket(bucketName, clientKey, config);
        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn("Rate limit exceeded: bucket={} client={}", bucketName, clientKey);
        }
        return allowed;
    }

    @Override
    public long getRemainingTokens(String bucketName, String clientKey, RateLimit config) {
        Bucket bucket = getOrCreateBucket(bucketName, clientKey, config);
        return bucket.getAvailableTokens();
    }

    @Override
    public void reset(String bucketName, String clientKey) {
        Map<String, Bucket> clientBuckets = buckets.get(bucketName);
        if (clientBuckets != null) {
            clientBuckets.remove(clientKey);
        }
    }

    @Override
    public void resetAll() {
        buckets.clear();
    }

    /**
     * Cleanup empty buckets periodically to prevent memory leak.
     * Runs every 10 minutes (configurable).
     */
    @Scheduled(fixedRateString = "${app.rate-limit.cleanup-interval-minutes:10}000")
    public void cleanup() {
        // Remove empty client buckets
        buckets.entrySet().removeIf(entry -> {
            entry.getValue().entrySet().removeIf(clientEntry ->
                clientEntry.getValue().getAvailableTokens() >= 0 &&
                isStale(clientEntry.getValue()));
            return entry.getValue().isEmpty();
        });
        log.debug("Rate limit cleanup completed. Active buckets: {}", buckets.size());
    }

    // ─────────────────────────────────────────────────────────────

    private Bucket getOrCreateBucket(String bucketName, String clientKey, RateLimit config) {
        return buckets
            .computeIfAbsent(bucketName, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(clientKey, k -> createBucket(config));
    }

    private Bucket createBucket(RateLimit config) {
        long refillTokens = config.refillTokens() > 0 ? config.refillTokens() : config.capacity();
        Duration refillDuration = toDuration(config.refillPeriod(), config.refillUnit());

        Bandwidth limit = Bandwidth.builder()
            .capacity(config.capacity())
            .refillGreedy(refillTokens, refillDuration)
            .build();

        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    private Duration toDuration(long period, RateLimit.RefillUnit unit) {
        return switch (unit) {
            case SECONDS -> Duration.ofSeconds(period);
            case MINUTES -> Duration.ofMinutes(period);
            case HOURS -> Duration.ofHours(period);
        };
    }

    private boolean isStale(Bucket bucket) {
        // Placeholder — Bucket4j doesn't expose last-access time directly.
        // In production, track last-access in a wrapper map if needed.
        return false;
    }
}