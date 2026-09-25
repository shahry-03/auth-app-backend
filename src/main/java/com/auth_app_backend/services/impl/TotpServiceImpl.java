package com.auth_app_backend.services.impl;

import com.auth_app_backend.services.TotpService;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Slf4j
@Service
public class TotpServiceImpl implements TotpService {

    private static final int SECRET_LENGTH = 32;
    private static final int CODE_DIGITS = 6;
    private static final int CODE_PERIOD_SECONDS = 30;

    private final SecretGenerator secretGenerator;
    private final CodeVerifier codeVerifier;
    private final QrGenerator qrGenerator;

    public TotpServiceImpl() {
        this.secretGenerator = new DefaultSecretGenerator(SECRET_LENGTH);
        this.codeVerifier = new DefaultCodeVerifier(
            new DefaultCodeGenerator(HashingAlgorithm.SHA1, CODE_DIGITS),
            new SystemTimeProvider()
        );
        this.qrGenerator = new ZxingPngQrGenerator();
    }

    @Override
    public String generateSecret() {
        return secretGenerator.generate();
    }

    @Override
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.length() != CODE_DIGITS) {
            return false;
        }
        try {
            return codeVerifier.isValidCode(secret, code);
        } catch (Exception e) {
            log.warn("TOTP verification error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String generateQrCodeDataUri(String secret, String email, String issuer) {
        QrData data = buildQrData(secret, email, issuer);

        try {
            byte[] imageData = qrGenerator.generate(data);
            String base64 = Base64.getEncoder().encodeToString(imageData);
            return "data:image/png;base64," + base64;
        } catch (QrGenerationException e) {
            log.error("QR generation failed: {}", e.getMessage());
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    @Override
    public String generateOtpAuthUrl(String secret, String email, String issuer) {
        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
            issuer, email, secret, issuer, CODE_DIGITS, CODE_PERIOD_SECONDS
        );
    }

    private QrData buildQrData(String secret, String email, String issuer) {
        return new QrData.Builder()
            .label(email)
            .secret(secret)
            .issuer(issuer)
            .algorithm(HashingAlgorithm.SHA1)
            .digits(CODE_DIGITS)
            .period(CODE_PERIOD_SECONDS)
            .build();
    }
}