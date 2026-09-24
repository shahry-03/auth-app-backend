package com.auth_app_backend.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitService Tests")
class RateLimitServiceTest {

    private RateLimitServiceImpl rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitServiceImpl();
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPER — Build RateLimit annotation instance
    // ═══════════════════════════════════════════════════════════

    private RateLimit config(String name, long capacity, long refillPeriod,
                             RateLimit.RefillUnit unit) {
        return new RateLimit() {
            @Override public String name() { return name; }
            @Override public long capacity() { return capacity; }
            @Override public long refillTokens() { return capacity; }
            @Override public long refillPeriod() { return refillPeriod; }
            @Override public RefillUnit refillUnit() { return unit; }
            @Override public Class<? extends Annotation> annotationType() {
                return RateLimit.class;
            }
        };
    }

    // ═══════════════════════════════════════════════════════════
    //  tryConsume
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("tryConsume()")
    class TryConsume {

        @Test
        @DisplayName("Should allow requests up to capacity")
        void shouldAllowUpToCapacity() {
            RateLimit cfg = config("login", 5, 15, RateLimit.RefillUnit.MINUTES);

            // 5 requests allowed
            for (int i = 1; i <= 5; i++) {
                boolean allowed = rateLimitService.tryConsume("login", "client-1", cfg);
                assertThat(allowed)
                    .as("Request #%d should be allowed", i)
                    .isTrue();
            }
        }

        @Test
        @DisplayName("Should reject request after capacity exceeded")
        void shouldRejectAfterCapacity() {
            RateLimit cfg = config("login", 5, 15, RateLimit.RefillUnit.MINUTES);

            // Consume all 5
            for (int i = 0; i < 5; i++) {
                rateLimitService.tryConsume("login", "client-1", cfg);
            }

            // 6th should be rejected
            boolean allowed = rateLimitService.tryConsume("login", "client-1", cfg);
            assertThat(allowed).isFalse();
        }

        @Test
        @DisplayName("Should have independent buckets for different clients")
        void shouldHaveIndependentBucketsPerClient() {
            RateLimit cfg = config("login", 2, 15, RateLimit.RefillUnit.MINUTES);

            // Client A — consume both
            rateLimitService.tryConsume("login", "client-A", cfg);
            rateLimitService.tryConsume("login", "client-A", cfg);
            assertThat(rateLimitService.tryConsume("login", "client-A", cfg)).isFalse();

            // Client B — fresh bucket, should be allowed
            assertThat(rateLimitService.tryConsume("login", "client-B", cfg)).isTrue();
        }

        @Test
        @DisplayName("Should have independent buckets for different bucket names")
        void shouldHaveIndependentBucketsPerName() {
            RateLimit loginCfg = config("login", 2, 15, RateLimit.RefillUnit.MINUTES);
            RateLimit registerCfg = config("register", 2, 60, RateLimit.RefillUnit.MINUTES);

            // Exhaust login bucket for client-1
            rateLimitService.tryConsume("login", "client-1", loginCfg);
            rateLimitService.tryConsume("login", "client-1", loginCfg);
            assertThat(rateLimitService.tryConsume("login", "client-1", loginCfg)).isFalse();

            // Register bucket should still have tokens
            assertThat(rateLimitService.tryConsume("register", "client-1", registerCfg)).isTrue();
        }

        @Test
        @DisplayName("Should handle capacity = 1")
        void shouldHandleSingleTokenCapacity() {
            RateLimit cfg = config("single", 1, 1, RateLimit.RefillUnit.MINUTES);

            assertThat(rateLimitService.tryConsume("single", "client-1", cfg)).isTrue();
            assertThat(rateLimitService.tryConsume("single", "client-1", cfg)).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  getRemainingTokens
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getRemainingTokens()")
    class GetRemainingTokens {

        @Test
        @DisplayName("Should return full capacity for new client")
        void shouldReturnFullCapacity() {
            RateLimit cfg = config("login", 5, 15, RateLimit.RefillUnit.MINUTES);

            long remaining = rateLimitService.getRemainingTokens("login", "new-client", cfg);
            assertThat(remaining).isEqualTo(5L);
        }

        @Test
        @DisplayName("Should decrease after consumption")
        void shouldDecreaseAfterConsumption() {
            RateLimit cfg = config("login", 5, 15, RateLimit.RefillUnit.MINUTES);

            rateLimitService.tryConsume("login", "client-1", cfg);
            rateLimitService.tryConsume("login", "client-1", cfg);

            long remaining = rateLimitService.getRemainingTokens("login", "client-1", cfg);
            assertThat(remaining).isEqualTo(3L);
        }

        @Test
        @DisplayName("Should not go below zero")
        void shouldNotGoBelowZero() {
            RateLimit cfg = config("login", 2, 15, RateLimit.RefillUnit.MINUTES);

            // Consume more than capacity
            rateLimitService.tryConsume("login", "client-1", cfg);
            rateLimitService.tryConsume("login", "client-1", cfg);
            rateLimitService.tryConsume("login", "client-1", cfg); // rejected

            long remaining = rateLimitService.getRemainingTokens("login", "client-1", cfg);
            assertThat(remaining).isEqualTo(0L);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  reset
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("reset() / resetAll()")
    class Reset {

        @Test
        @DisplayName("reset() should clear bucket for one client")
        void shouldResetOneClient() {
            RateLimit cfg = config("login", 3, 15, RateLimit.RefillUnit.MINUTES);

            // Exhaust bucket
            rateLimitService.tryConsume("login", "client-1", cfg);
            rateLimitService.tryConsume("login", "client-1", cfg);
            rateLimitService.tryConsume("login", "client-1", cfg);
            assertThat(rateLimitService.tryConsume("login", "client-1", cfg)).isFalse();

            // Reset
            rateLimitService.reset("login", "client-1");

            // Fresh bucket — should be allowed
            assertThat(rateLimitService.tryConsume("login", "client-1", cfg)).isTrue();
            long remaining = rateLimitService.getRemainingTokens("login", "client-1", cfg);
            assertThat(remaining).isEqualTo(2L);
        }

        @Test
        @DisplayName("resetAll() should clear all buckets")
        void shouldResetAll() {
            RateLimit cfg = config("login", 2, 15, RateLimit.RefillUnit.MINUTES);

            // Exhaust 3 clients
            for (String client : new String[]{"a", "b", "c"}) {
                rateLimitService.tryConsume("login", client, cfg);
                rateLimitService.tryConsume("login", client, cfg);
                assertThat(rateLimitService.tryConsume("login", client, cfg)).isFalse();
            }

            // Reset all
            rateLimitService.resetAll();

            // All clients should be fresh
            for (String client : new String[]{"a", "b", "c"}) {
                assertThat(rateLimitService.tryConsume("login", client, cfg))
                    .as("Client %s should be allowed after resetAll", client)
                    .isTrue();
            }
        }

        @Test
        @DisplayName("reset() on non-existent client should not throw")
        void shouldNotThrowOnUnknownClient() {
            RateLimit cfg = config("login", 3, 15, RateLimit.RefillUnit.MINUTES);

            // Should silently do nothing
            rateLimitService.reset("login", "unknown-client");

            long remaining = rateLimitService.getRemainingTokens("login", "unknown-client", cfg);
            assertThat(remaining).isEqualTo(3L);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  Different RefillUnits
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Refill Units")
    class RefillUnits {

        @Test
        @DisplayName("Should work with SECONDS refill")
        void shouldWorkWithSeconds() throws InterruptedException {
            RateLimit cfg = config("fast", 1, 1, RateLimit.RefillUnit.SECONDS);

            // Consume 1
            assertThat(rateLimitService.tryConsume("fast", "client-1", cfg)).isTrue();
            assertThat(rateLimitService.tryConsume("fast", "client-1", cfg)).isFalse();

            // Wait 1.1 seconds for refill
            Thread.sleep(1100);

            // Should be allowed again
            assertThat(rateLimitService.tryConsume("fast", "client-1", cfg)).isTrue();
        }

        @Test
        @DisplayName("Should work with HOURS refill")
        void shouldWorkWithHours() {
            RateLimit cfg = config("hourly", 3, 1, RateLimit.RefillUnit.HOURS);

            for (int i = 0; i < 3; i++) {
                assertThat(rateLimitService.tryConsume("hourly", "client-1", cfg)).isTrue();
            }
            assertThat(rateLimitService.tryConsume("hourly", "client-1", cfg)).isFalse();
        }
    }
}