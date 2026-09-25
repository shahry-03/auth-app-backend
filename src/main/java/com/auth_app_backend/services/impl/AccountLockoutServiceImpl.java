package com.auth_app_backend.services.impl;

import com.auth_app_backend.config.AccountLockoutProperties;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.exception.AccountLockedException;
import com.auth_app_backend.repositories.UserRepository;
import com.auth_app_backend.services.AccountLockoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountLockoutServiceImpl implements AccountLockoutService {

    private final UserRepository userRepository;
    private final AccountLockoutProperties properties;

    // ═══════════════════════════════════════════════════════════
    //  CHECK LOCK
    // ═══════════════════════════════════════════════════════════

    @Override
    public void checkAccountLocked(User user) {
        if (!properties.isEnabled()) return;

        // Clear expired lock
        if (user.isLockExpired()) {
            log.info("Lock expired for user: {}, auto-unlocking", user.getId());
            clearLock(user);
            return;
        }

        // Still locked?
        if (user.isLocked()) {
            long minutesLeft = ChronoUnit.MINUTES.between(
                Instant.now(), user.getLockedUntil());
            throw new AccountLockedException(
                String.format("Account is locked. Try again in %d minute(s).", minutesLeft),
                user.getLockedUntil()
            );
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  RECORD FAILED ATTEMPT
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) 
    public boolean recordFailedAttempt(User user) {
        if (!properties.isEnabled()) return false;

        // Reset counter if last failure was too long ago
        Instant now = Instant.now();
        if (user.getLastFailedLogin() != null) {
            long minutesSinceLastFailure = ChronoUnit.MINUTES.between(
                user.getLastFailedLogin(), now);
            if (minutesSinceLastFailure >= properties.getResetCounterAfterMinutes()) {
                log.debug("Resetting stale failed counter for user: {}", user.getId());
                user.setFailedLoginAttempts(0);
            }
        }

        // Increment counter
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        user.setLastFailedLogin(now);

        // Check threshold
        boolean justLocked = false;
        if (attempts >= properties.getMaxFailedAttempts()) {
            Instant lockUntil = now.plus(
                properties.getLockoutDurationMinutes(), ChronoUnit.MINUTES);
            user.setLockedUntil(lockUntil);
            justLocked = true;
            log.warn("Account LOCKED for user: {} until {}", user.getId(), lockUntil);
        } else {
            log.info("Failed login attempt {} of {} for user: {}",
                attempts, properties.getMaxFailedAttempts(), user.getId());
        }

        userRepository.saveAndFlush(user);
        return justLocked;
    }

    // ═══════════════════════════════════════════════════════════
    //  RESET COUNTER
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetFailedAttempts(User user) {
        if (user.getFailedLoginAttempts() == 0 && user.getLockedUntil() == null) {
            return; // Nothing to reset
        }
        clearLock(user);
        log.debug("Reset failed attempts for user: {}", user.getId());
    }

    // ═══════════════════════════════════════════════════════════
    //  MANUAL UNLOCK
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unlockAccount(User user) {
        clearLock(user);
        log.info("Account manually UNLOCKED for user: {}", user.getId());
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPER
    // ═══════════════════════════════════════════════════════════

    private void clearLock(User user) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastFailedLogin(null);
        userRepository.saveAndFlush(user);
    }
}