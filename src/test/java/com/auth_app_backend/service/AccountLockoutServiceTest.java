package com.auth_app_backend.service;

import com.auth_app_backend.config.AccountLockoutProperties;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.exception.AccountLockedException;
import com.auth_app_backend.repositories.UserRepository;
import com.auth_app_backend.services.impl.AccountLockoutServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) 
@DisplayName("AccountLockoutService Tests")
class AccountLockoutServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AccountLockoutProperties properties;

    @InjectMocks
    private AccountLockoutServiceImpl accountLockoutService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .failedLoginAttempts(0)
            .lockedUntil(null)
            .lastFailedLogin(null)
            .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  checkAccountLocked
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("checkAccountLocked()")
    class CheckAccountLocked {

        @Test
        @DisplayName("Should pass when account not locked")
        void shouldPassWhenNotLocked() {
            when(properties.isEnabled()).thenReturn(true);

            accountLockoutService.checkAccountLocked(testUser);
            // No exception
        }

        @Test
        @DisplayName("Should throw when account is currently locked")
        void shouldThrowWhenLocked() {
            when(properties.isEnabled()).thenReturn(true);
            testUser.setLockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES));

            assertThatThrownBy(() -> accountLockoutService.checkAccountLocked(testUser))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("Account is locked");
        }

        @Test
        @DisplayName("Should auto-clear expired lock")
        void shouldAutoClearExpiredLock() {
            when(properties.isEnabled()).thenReturn(true);
            testUser.setLockedUntil(Instant.now().minus(1, ChronoUnit.MINUTES));
            testUser.setFailedLoginAttempts(5);

            accountLockoutService.checkAccountLocked(testUser);

            assertThat(testUser.getFailedLoginAttempts()).isZero();
            assertThat(testUser.getLockedUntil()).isNull();
            verify(userRepository).saveAndFlush(testUser);
        }

        @Test
        @DisplayName("Should skip check when disabled")
        void shouldSkipWhenDisabled() {
            when(properties.isEnabled()).thenReturn(false);
            testUser.setLockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES));

            // No exception despite lock
            accountLockoutService.checkAccountLocked(testUser);

            verify(userRepository, never()).saveAndFlush(any());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  recordFailedAttempt
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("recordFailedAttempt()")
    class RecordFailedAttempt {

        @Test
        @DisplayName("Should increment counter on first failure")
        void shouldIncrementOnFirstFailure() {
            when(properties.isEnabled()).thenReturn(true);
            when(properties.getMaxFailedAttempts()).thenReturn(5);
            when(properties.getResetCounterAfterMinutes()).thenReturn(30);
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);

            boolean justLocked = accountLockoutService.recordFailedAttempt(testUser);

            assertThat(justLocked).isFalse();
            assertThat(testUser.getFailedLoginAttempts()).isEqualTo(1);
            assertThat(testUser.getLastFailedLogin()).isNotNull();
            assertThat(testUser.getLockedUntil()).isNull();
        }

        @Test
        @DisplayName("Should lock account on 5th failed attempt")
        void shouldLockOn5thFailure() {
            when(properties.isEnabled()).thenReturn(true);
            when(properties.getMaxFailedAttempts()).thenReturn(5);
            when(properties.getLockoutDurationMinutes()).thenReturn(15);
            when(properties.getResetCounterAfterMinutes()).thenReturn(30);
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);

            testUser.setFailedLoginAttempts(4);

            boolean justLocked = accountLockoutService.recordFailedAttempt(testUser);

            assertThat(justLocked).isTrue();
            assertThat(testUser.getFailedLoginAttempts()).isEqualTo(5);
            assertThat(testUser.getLockedUntil()).isNotNull();
            assertThat(testUser.getLockedUntil()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("Should reset counter if last failure was stale")
        void shouldResetStaleCounter() {
            when(properties.isEnabled()).thenReturn(true);
            when(properties.getMaxFailedAttempts()).thenReturn(5);
            when(properties.getResetCounterAfterMinutes()).thenReturn(30);
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);

            testUser.setFailedLoginAttempts(3);
            testUser.setLastFailedLogin(Instant.now().minus(60, ChronoUnit.MINUTES)); // 60 min ago

            accountLockoutService.recordFailedAttempt(testUser);

            // Should reset to 0 then increment to 1
            assertThat(testUser.getFailedLoginAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should not reset if last failure recent")
        void shouldNotResetRecentCounter() {
            when(properties.isEnabled()).thenReturn(true);
            when(properties.getMaxFailedAttempts()).thenReturn(5);
            when(properties.getResetCounterAfterMinutes()).thenReturn(30);
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);

            testUser.setFailedLoginAttempts(3);
            testUser.setLastFailedLogin(Instant.now().minus(5, ChronoUnit.MINUTES)); // 5 min ago

            accountLockoutService.recordFailedAttempt(testUser);

            assertThat(testUser.getFailedLoginAttempts()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should skip when disabled")
        void shouldSkipWhenDisabled() {
            when(properties.isEnabled()).thenReturn(false);

            boolean result = accountLockoutService.recordFailedAttempt(testUser);

            assertThat(result).isFalse();
            verify(userRepository, never()).saveAndFlush(any());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  resetFailedAttempts
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("resetFailedAttempts()")
    class ResetFailedAttempts {

        @Test
        @DisplayName("Should clear all lockout fields")
        void shouldClearAllFields() {
            testUser.setFailedLoginAttempts(3);
            testUser.setLockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES));
            testUser.setLastFailedLogin(Instant.now());
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);

            accountLockoutService.resetFailedAttempts(testUser);

            assertThat(testUser.getFailedLoginAttempts()).isZero();
            assertThat(testUser.getLockedUntil()).isNull();
            assertThat(testUser.getLastFailedLogin()).isNull();
            verify(userRepository).saveAndFlush(testUser);
        }

        @Test
        @DisplayName("Should skip when already clean")
        void shouldSkipWhenClean() {
            accountLockoutService.resetFailedAttempts(testUser);

            verify(userRepository, never()).saveAndFlush(any());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  unlockAccount
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("unlockAccount()")
    class UnlockAccount {

        @Test
        @DisplayName("Should clear lock and save")
        void shouldClearLock() {
            testUser.setFailedLoginAttempts(5);
            testUser.setLockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES));
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);

            accountLockoutService.unlockAccount(testUser);

            assertThat(testUser.getFailedLoginAttempts()).isZero();
            assertThat(testUser.getLockedUntil()).isNull();
            verify(userRepository).saveAndFlush(testUser);
        }
    }
}