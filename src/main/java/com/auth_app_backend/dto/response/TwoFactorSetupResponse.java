package com.auth_app_backend.dto.response;

/**
 * Response from /2fa/setup — contains secret + QR code.
 * User scans QR with Google Authenticator, then calls /2fa/enable.
 */
public record TwoFactorSetupResponse(
    String secret,            // Base32 secret (for manual entry)
    String qrCodeDataUri,     // data:image/png;base64,...
    String otpAuthUrl         // otpauth:// URI
) {}