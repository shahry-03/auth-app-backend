package com.auth_app_backend.controllers;

import com.auth_app_backend.services.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * DEV ONLY — Test endpoint for email service.
 * Disabled in production via @Profile("dev").
 */
@RestController
@RequestMapping("/dev/email")
@Profile("dev")
@RequiredArgsConstructor
public class DevEmailTestController {

    private final EmailService emailService;

    @GetMapping("/verification")
    public String testVerification(@RequestParam String to) {
        emailService.sendVerificationEmail(to, "Test User", "test-token-verify-123");
        return "Verification email sent to " + to + " — check Mailtrap";
    }

    @GetMapping("/reset")
    public String testReset(@RequestParam String to) {
        emailService.sendPasswordResetEmail(to, "Test User", "test-token-reset-456");
        return "Password reset email sent to " + to + " — check Mailtrap";
    }

    @GetMapping("/welcome")
    public String testWelcome(@RequestParam String to) {
        emailService.sendWelcomeEmail(to, "Test User");
        return "Welcome email sent to " + to + " — check Mailtrap";
    }
}