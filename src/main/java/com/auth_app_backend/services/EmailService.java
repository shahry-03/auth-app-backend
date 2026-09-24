package com.auth_app_backend.services;   

public interface EmailService {

    void sendVerificationEmail(String toEmail, String userName, String verificationToken);

    void sendPasswordResetEmail(String toEmail, String userName, String resetToken);

    void sendWelcomeEmail(String toEmail, String userName);

    void sendPasswordChangedEmail(String toEmail, String userName);
}