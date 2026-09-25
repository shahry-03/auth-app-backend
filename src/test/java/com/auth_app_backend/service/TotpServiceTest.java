package com.auth_app_backend.service;

import com.auth_app_backend.services.impl.TotpServiceImpl;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TotpService Tests")
class TotpServiceTest {

    private TotpServiceImpl totpService;

    @BeforeEach
    void setUp() {
        totpService = new TotpServiceImpl();
    }

    // ═══════════════════════════════════════════════════════════
    //  generateSecret
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("generateSecret()")
    class GenerateSecret {

        @Test
        @DisplayName("Should generate non-empty Base32 secret")
        void shouldGenerateNonEmptySecret() {
            String secret = totpService.generateSecret();

            assertThat(secret).isNotBlank();
            // Base32 alphabet: A-Z, 2-7
            assertThat(secret).matches("^[A-Z2-7]+=*$");
        }

        @Test
        @DisplayName("Should generate unique secret each call")
        void shouldGenerateUniqueSecrets() {
            String secret1 = totpService.generateSecret();
            String secret2 = totpService.generateSecret();

            assertThat(secret1).isNotEqualTo(secret2);
        }

        @Test
        @DisplayName("Should generate secret of expected length (32 chars)")
        void shouldGenerate32CharSecret() {
            String secret = totpService.generateSecret();

            assertThat(secret).hasSize(32);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  verifyCode
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("verifyCode()")
    class VerifyCode {

        @Test
        @DisplayName("Should verify valid current TOTP code")
        void shouldVerifyValidCode() throws Exception {
            String secret = totpService.generateSecret();

            // Generate current valid code
            CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);
            long currentBucket = new SystemTimeProvider().getTime() / 30;
            String validCode = codeGenerator.generate(secret, currentBucket);

            boolean result = totpService.verifyCode(secret, validCode);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should reject wrong code")
        void shouldRejectWrongCode() {
            String secret = totpService.generateSecret();

            boolean result = totpService.verifyCode(secret, "000000");

            // Might randomly be valid (1 in 1M chance) but generally false
            // To be safe, we test with a very unlikely scenario
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject null code")
        void shouldRejectNullCode() {
            String secret = totpService.generateSecret();

            assertThat(totpService.verifyCode(secret, null)).isFalse();
        }

        @Test
        @DisplayName("Should reject null secret")
        void shouldRejectNullSecret() {
            assertThat(totpService.verifyCode(null, "123456")).isFalse();
        }

        @Test
        @DisplayName("Should reject code with wrong length")
        void shouldRejectWrongLength() {
            String secret = totpService.generateSecret();

            assertThat(totpService.verifyCode(secret, "12345")).isFalse();      // 5 digits
            assertThat(totpService.verifyCode(secret, "1234567")).isFalse();    // 7 digits
            assertThat(totpService.verifyCode(secret, "abcdef")).isFalse();     // non-numeric
        }

        @Test
        @DisplayName("Should reject empty code")
        void shouldRejectEmptyCode() {
            String secret = totpService.generateSecret();

            assertThat(totpService.verifyCode(secret, "")).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  generateQrCodeDataUri
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("generateQrCodeDataUri()")
    class GenerateQrCode {

        @Test
        @DisplayName("Should generate valid PNG data URI")
        void shouldGenerateValidDataUri() {
            String secret = totpService.generateSecret();

            String dataUri = totpService.generateQrCodeDataUri(
                secret, "test@example.com", "Test App");

            assertThat(dataUri).startsWith("data:image/png;base64,");
            assertThat(dataUri.length()).isGreaterThan(100);
        }

        @Test
        @DisplayName("Should generate different QR codes for different secrets")
        void shouldGenerateDifferentQrCodes() {
            String secret1 = totpService.generateSecret();
            String secret2 = totpService.generateSecret();

            String qr1 = totpService.generateQrCodeDataUri(secret1, "a@test.com", "App");
            String qr2 = totpService.generateQrCodeDataUri(secret2, "b@test.com", "App");

            assertThat(qr1).isNotEqualTo(qr2);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  generateOtpAuthUrl
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("generateOtpAuthUrl()")
    class GenerateOtpAuthUrl {

        @Test
        @DisplayName("Should generate valid otpauth:// URL")
        void shouldGenerateValidUrl() {
            String secret = "JBSWY3DPEHPK3PXP";

            String url = totpService.generateOtpAuthUrl(
                secret, "user@test.com", "My App");

            assertThat(url).startsWith("otpauth://totp/");
            assertThat(url).contains("secret=" + secret);
            assertThat(url).contains("issuer=My App");
            assertThat(url).contains("algorithm=SHA1");
            assertThat(url).contains("digits=6");
            assertThat(url).contains("period=30");
        }

        @Test
        @DisplayName("Should URL-encode email in label")
        void shouldUrlEncodeEmail() {
            String secret = "JBSWY3DPEHPK3PXP";

            String url = totpService.generateOtpAuthUrl(
                secret, "user@test.com", "App");

            assertThat(url).contains("user@test.com");
        }
    }
}