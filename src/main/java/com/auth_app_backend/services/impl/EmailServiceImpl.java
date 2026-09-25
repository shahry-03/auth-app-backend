package com.auth_app_backend.services.impl;

import com.auth_app_backend.services.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    @Async
    public void sendVerificationEmail(String toEmail, String userName, String token) {
        String link = frontendBaseUrl + "/verify-email?token=" + token;

        Context ctx = new Context();
        ctx.setVariable("userName", userName);
        ctx.setVariable("verificationLink", link);
        ctx.setVariable("expiryHours", 24);

        sendHtmlEmail(toEmail, "Verify your email", "email/verify-email", ctx);
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String userName, String token) {
        String link = frontendBaseUrl + "/reset-password?token=" + token;

        Context ctx = new Context();
        ctx.setVariable("userName", userName);
        ctx.setVariable("resetLink", link);
        ctx.setVariable("expiryMinutes", 30);

        sendHtmlEmail(toEmail, "Reset your password", "email/reset-password", ctx);
    }

    @Override
    @Async
    public void sendWelcomeEmail(String toEmail, String userName) {
        Context ctx = new Context();
        ctx.setVariable("userName", userName);
        sendHtmlEmail(toEmail, "Welcome to Universal Auth", "email/welcome", ctx);
    }

    @Override
    @Async
    public void sendPasswordChangedEmail(String toEmail, String userName) {
        Context ctx = new Context();
        ctx.setVariable("userName", userName);
        sendHtmlEmail(toEmail, "Password changed successfully", "email/password-changed", ctx);
    }

    // ─────────────────────────────────────────────

    private void sendHtmlEmail(String to, String subject, String template, Context ctx) {
        try {
            String html = templateEngine.process(template, ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Email sent [{}] → {}", template, to);

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send email [{}] to {}: {}", template, to, e.getMessage());
            // Don't rethrow — email failure shouldn't break the flow
        }
    }


    @Override
    @Async
    public void sendAccountLockedEmail(String toEmail, String userName,
                                        int maxAttempts, int lockDurationMinutes) {
        Context ctx = new Context();
        ctx.setVariable("userName", userName);
        ctx.setVariable("maxAttempts", maxAttempts);
        ctx.setVariable("lockDurationMinutes", lockDurationMinutes);
        ctx.setVariable("resetPasswordLink", frontendBaseUrl + "/forgot-password");
        sendHtmlEmail(toEmail, "⚠️ Your account has been locked",
            "email/account-locked", ctx);
    }

    @Override
    @Async
    public void sendAccountUnlockedEmail(String toEmail, String userName) {
        Context ctx = new Context();
        ctx.setVariable("userName", userName);
        sendHtmlEmail(toEmail, "Your account has been unlocked",
            "email/account-unlocked", ctx);
    }
}