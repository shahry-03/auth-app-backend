package com.auth_app_backend.services;

public interface TotpService {

    /**
     * Generate a new random TOTP secret (Base32 encoded).
     */
    String generateSecret();

    /**
     * Verify a 6-digit TOTP code against the secret.
     */
    boolean verifyCode(String secret, String code);

    /**
     * Generate QR code as data URI (base64 PNG).
     * Client can render directly in <img src="..."> tag.
     */
    String generateQrCodeDataUri(String secret, String email, String issuer);

    /**
     * Generate otpauth:// URI (for manual entry or client-side QR generation).
     */
    String generateOtpAuthUrl(String secret, String email, String issuer);
}