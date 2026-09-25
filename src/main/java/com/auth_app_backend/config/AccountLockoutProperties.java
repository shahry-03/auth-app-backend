package com.auth_app_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security.account-lockout")
public class AccountLockoutProperties {

    /**
     * Enable/disable account lockout feature.
     */
    private boolean enabled = true;

    /**
     * Number of failed attempts before locking the account.
     */
    private int maxFailedAttempts = 5;

    /**
     * Duration (in minutes) for which the account stays locked.
     */
    private int lockoutDurationMinutes = 15;

    /**
     * After this many minutes of no failed attempts, reset the counter.
     * Prevents permanent lock from slow attacker.
     */
    private int resetCounterAfterMinutes = 30;

    /**
     * Send email notification when account is locked.
     */
    private boolean notifyOnLock = true;
}