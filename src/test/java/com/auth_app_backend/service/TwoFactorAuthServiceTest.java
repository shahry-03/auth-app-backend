package com.auth_app_backend.service;

import com.auth_app_backend.dto.request.TwoFactorDisableRequest;
import com.auth_app_backend.dto.request.TwoFactorEnableRequest;
import com.auth_app_backend.dto.request.TwoFactorVerifyRequest;
import com.auth_app_backend.dto.response.TokenResponse;
import com.auth_app_backend.dto.response.TwoFactorEnableResponse;
import com.auth_app_backend.dto.response.TwoFactorSetupResponse;
import com.auth_app_backend.dto.response.TwoFactorStatusResponse;
import com.auth_app_backend.entity.RefreshToken;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.repositories.UserRepository;
import com.auth_app_backend.security.JwtService;
import com.auth_app_backend.services.BackupCodeService;
import com.auth_app_backend.services.RefreshTokenService;
import com.auth_app_backend.services.TotpService;
import com.auth_app_backend.services.impl.TwoFactorAuthServiceImpl;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TwoFactorAuthService Tests")
class TwoFactorAuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TotpService totpService;
    @Mock private BackupCodeService backupCodeService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks
    private TwoFactorAuthServiceImpl twoFactorAuthService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Inject @Value field
        ReflectionTestUtils.setField(twoFactorAuthService, "issuer", "Test App");

        testUser = User.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .name("Test User")
            .password("hashed-password")
            .enabled(true)
            .emailVerified(true)
            .twoFactorEnabled(false)
            .totpSecret(null)
            .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  SETUP
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("setup()")
    class Setup {

        @Test
        @DisplayName("Should generate secret and QR code")
        void shouldGenerateSecretAndQr() {
            String secret = "JBSWY3DPEHPK3PXP";
            String qrUri = "data:image/png;base64,xxx";
            String otpUrl = "otpauth://totp/...";

            when(totpService.generateSecret()).thenReturn(secret);
            when(totpService.generateQrCodeDataUri(secret, testUser.getEmail(), "Test App"))
                .thenReturn(qrUri);
            when(totpService.generateOtpAuthUrl(secret, testUser.getEmail(), "Test App"))
                .thenReturn(otpUrl);
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);

            TwoFactorSetupResponse result = twoFactorAuthService.setup(testUser);

            assertThat(result.secret()).isEqualTo(secret);
            assertThat(result.qrCodeDataUri()).isEqualTo(qrUri);
            assertThat(result.otpAuthUrl()).isEqualTo(otpUrl);
        }

        @Test
        @DisplayName("Should save secret to user but NOT enable 2FA")
        void shouldSaveSecretButNotEnable() {
            when(totpService.generateSecret()).thenReturn("NEW-SECRET");
            when(totpService.generateQrCodeDataUri(anyString(), anyString(), anyString()))
                .thenReturn("data:image/png;base64,xxx");
            when(totpService.generateOtpAuthUrl(anyString(), anyString(), anyString()))
                .thenReturn("otpauth://...");
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);

            twoFactorAuthService.setup(testUser);

            assertThat(testUser.getTotpSecret()).isEqualTo("NEW-SECRET");
            assertThat(testUser.isTwoFactorEnabled()).isFalse();
            verify(userRepository).saveAndFlush(testUser);
        }

        @Test
        @DisplayName("Should throw when 2FA already enabled")
        void shouldThrowWhenAlreadyEnabled() {
            testUser.setTwoFactorEnabled(true);

            assertThatThrownBy(() -> twoFactorAuthService.setup(testUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already enabled");

            verify(userRepository, never()).saveAndFlush(any());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  ENABLE
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("enable()")
    class Enable {

        @BeforeEach
        void setUpSecret() {
            testUser.setTotpSecret("VALID-SECRET");
        }

        @Test
        @DisplayName("Should enable 2FA and generate backup codes")
        void shouldEnableAndGenerateBackupCodes() {
            TwoFactorEnableRequest request = new TwoFactorEnableRequest("123456");
            List<String> backupCodes = List.of("A1B2-C3D4-E5F6", "G7H8-J9K2-L3M4");

            when(totpService.verifyCode("VALID-SECRET", "123456")).thenReturn(true);
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);
            when(backupCodeService.generateCodes(testUser, 10)).thenReturn(backupCodes);

            TwoFactorEnableResponse result = twoFactorAuthService.enable(testUser, request);

            assertThat(result.enabled()).isTrue();
            assertThat(result.backupCodes()).hasSize(2);
            assertThat(testUser.isTwoFactorEnabled()).isTrue();
            verify(backupCodeService).generateCodes(testUser, 10);
        }

        @Test
        @DisplayName("Should throw when code is invalid")
        void shouldThrowOnInvalidCode() {
            TwoFactorEnableRequest request = new TwoFactorEnableRequest("000000");

            when(totpService.verifyCode("VALID-SECRET", "000000")).thenReturn(false);

            assertThatThrownBy(() -> twoFactorAuthService.enable(testUser, request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid verification code");

            assertThat(testUser.isTwoFactorEnabled()).isFalse();
            verify(userRepository, never()).saveAndFlush(any());
            verify(backupCodeService, never()).generateCodes(any(), anyInt());
        }

        @Test
        @DisplayName("Should throw when setup not called (no secret)")
        void shouldThrowWhenNoSecret() {
            testUser.setTotpSecret(null);
            TwoFactorEnableRequest request = new TwoFactorEnableRequest("123456");

            assertThatThrownBy(() -> twoFactorAuthService.enable(testUser, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Call /2fa/setup first");
        }

        @Test
        @DisplayName("Should throw when 2FA already enabled")
        void shouldThrowWhenAlreadyEnabled() {
            testUser.setTwoFactorEnabled(true);
            TwoFactorEnableRequest request = new TwoFactorEnableRequest("123456");

            assertThatThrownBy(() -> twoFactorAuthService.enable(testUser, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already enabled");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  DISABLE
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("disable()")
    class Disable {

        @BeforeEach
        void enableTwoFactor() {
            testUser.setTwoFactorEnabled(true);
            testUser.setTotpSecret("VALID-SECRET");
        }

        @Test
        @DisplayName("Should disable 2FA with correct password + code")
        void shouldDisableSuccessfully() {
            TwoFactorDisableRequest request = new TwoFactorDisableRequest("PlainPass@123", "123456");

            when(passwordEncoder.matches("PlainPass@123", "hashed-password")).thenReturn(true);
            when(totpService.verifyCode("VALID-SECRET", "123456")).thenReturn(true);
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);

            twoFactorAuthService.disable(testUser, request);

            assertThat(testUser.isTwoFactorEnabled()).isFalse();
            assertThat(testUser.getTotpSecret()).isNull();
            verify(backupCodeService).invalidateAll(testUser);
            verify(refreshTokenService).revokeAllForUser(testUser.getId());
        }

        @Test
        @DisplayName("Should throw when password wrong")
        void shouldThrowOnWrongPassword() {
            TwoFactorDisableRequest request = new TwoFactorDisableRequest("WrongPass", "123456");

            when(passwordEncoder.matches("WrongPass", "hashed-password")).thenReturn(false);

            assertThatThrownBy(() -> twoFactorAuthService.disable(testUser, request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid password");

            assertThat(testUser.isTwoFactorEnabled()).isTrue();
            verify(userRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Should throw when code invalid")
        void shouldThrowOnInvalidCode() {
            TwoFactorDisableRequest request = new TwoFactorDisableRequest("PlainPass@123", "000000");

            when(passwordEncoder.matches("PlainPass@123", "hashed-password")).thenReturn(true);
            when(totpService.verifyCode("VALID-SECRET", "000000")).thenReturn(false);

            assertThatThrownBy(() -> twoFactorAuthService.disable(testUser, request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid verification code");

            assertThat(testUser.isTwoFactorEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should throw when 2FA not enabled")
        void shouldThrowWhenNotEnabled() {
            testUser.setTwoFactorEnabled(false);
            TwoFactorDisableRequest request = new TwoFactorDisableRequest("pass", "123456");

            assertThatThrownBy(() -> twoFactorAuthService.disable(testUser, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not enabled");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  STATUS
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getStatus()")
    class GetStatus {

        @Test
        @DisplayName("Should return enabled + backup codes count")
        void shouldReturnStatus() {
            testUser.setTwoFactorEnabled(true);
            when(backupCodeService.countRemaining(testUser)).thenReturn(7L);

            TwoFactorStatusResponse result = twoFactorAuthService.getStatus(testUser);

            assertThat(result.enabled()).isTrue();
            assertThat(result.backupCodesRemaining()).isEqualTo(7L);
        }

        @Test
        @DisplayName("Should return disabled when 2FA off")
        void shouldReturnDisabled() {
            when(backupCodeService.countRemaining(testUser)).thenReturn(0L);

            TwoFactorStatusResponse result = twoFactorAuthService.getStatus(testUser);

            assertThat(result.enabled()).isFalse();
            assertThat(result.backupCodesRemaining()).isZero();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  VERIFY (LOGIN STEP 2)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("verifyAndCompleteLogin()")
    class VerifyAndCompleteLogin {

        private static final String TEMP_TOKEN = "valid-temp-token";

        @BeforeEach
        void enableTwoFactor() {
            testUser.setTwoFactorEnabled(true);
            testUser.setTotpSecret("VALID-SECRET");
        }

        @Test
        @DisplayName("Should complete login with valid TOTP code")
        void shouldCompleteLoginWithTotp() {
            TwoFactorVerifyRequest request = new TwoFactorVerifyRequest(TEMP_TOKEN, "123456");

            RefreshToken refreshToken = RefreshToken.builder().jti("jti-123").user(testUser).build();

            when(jwtService.isTwoFactorTempToken(TEMP_TOKEN)).thenReturn(true);
            when(jwtService.getUserId(TEMP_TOKEN)).thenReturn(testUser.getId());
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(totpService.verifyCode("VALID-SECRET", "123456")).thenReturn(true);
            when(refreshTokenService.createForUser(testUser)).thenReturn(refreshToken);
            when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
            when(jwtService.generateRefreshToken(testUser, "jti-123")).thenReturn("refresh-token");
            when(jwtService.getJwtExpirationInMillis()).thenReturn(3600000L);

            TokenResponse result = twoFactorAuthService.verifyAndCompleteLogin(request);

            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
            assertThat(result.requiresTwoFactor()).isNull();
        }

        @Test
        @DisplayName("Should complete login with backup code")
        void shouldCompleteLoginWithBackupCode() {
            TwoFactorVerifyRequest request = new TwoFactorVerifyRequest(TEMP_TOKEN, "A1B2-C3D4-E5F6");

            RefreshToken refreshToken = RefreshToken.builder().jti("jti-123").user(testUser).build();

            when(jwtService.isTwoFactorTempToken(TEMP_TOKEN)).thenReturn(true);
            when(jwtService.getUserId(TEMP_TOKEN)).thenReturn(testUser.getId());
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(backupCodeService.verifyAndConsume(testUser, "A1B2-C3D4-E5F6")).thenReturn(true);
            when(refreshTokenService.createForUser(testUser)).thenReturn(refreshToken);
            when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
            when(jwtService.generateRefreshToken(testUser, "jti-123")).thenReturn("refresh-token");
            when(jwtService.getJwtExpirationInMillis()).thenReturn(3600000L);

            TokenResponse result = twoFactorAuthService.verifyAndCompleteLogin(request);

            assertThat(result.accessToken()).isEqualTo("access-token");
            verify(backupCodeService).verifyAndConsume(testUser, "A1B2-C3D4-E5F6");
        }

        @Test
        @DisplayName("Should throw when temp token invalid")
        void shouldThrowOnInvalidTempToken() {
            TwoFactorVerifyRequest request = new TwoFactorVerifyRequest("bad-token", "123456");

            when(jwtService.isTwoFactorTempToken("bad-token")).thenReturn(false);

            assertThatThrownBy(() -> twoFactorAuthService.verifyAndCompleteLogin(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid or expired temporary token");
        }

        @Test
        @DisplayName("Should throw when TOTP code invalid")
        void shouldThrowOnInvalidTotp() {
            TwoFactorVerifyRequest request = new TwoFactorVerifyRequest(TEMP_TOKEN, "000000");

            when(jwtService.isTwoFactorTempToken(TEMP_TOKEN)).thenReturn(true);
            when(jwtService.getUserId(TEMP_TOKEN)).thenReturn(testUser.getId());
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(totpService.verifyCode("VALID-SECRET", "000000")).thenReturn(false);

            assertThatThrownBy(() -> twoFactorAuthService.verifyAndCompleteLogin(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid 2FA code");

            verify(refreshTokenService, never()).createForUser(any());
        }

        @Test
        @DisplayName("Should throw when backup code invalid")
        void shouldThrowOnInvalidBackupCode() {
            TwoFactorVerifyRequest request = new TwoFactorVerifyRequest(TEMP_TOKEN, "WRONG-CODE-XXXX");

            when(jwtService.isTwoFactorTempToken(TEMP_TOKEN)).thenReturn(true);
            when(jwtService.getUserId(TEMP_TOKEN)).thenReturn(testUser.getId());
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(backupCodeService.verifyAndConsume(testUser, "WRONG-CODE-XXXX")).thenReturn(false);

            assertThatThrownBy(() -> twoFactorAuthService.verifyAndCompleteLogin(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid 2FA code");
        }

        @Test
        @DisplayName("Should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            UUID randomId = UUID.randomUUID();
            TwoFactorVerifyRequest request = new TwoFactorVerifyRequest(TEMP_TOKEN, "123456");

            when(jwtService.isTwoFactorTempToken(TEMP_TOKEN)).thenReturn(true);
            when(jwtService.getUserId(TEMP_TOKEN)).thenReturn(randomId);
            when(userRepository.findById(randomId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> twoFactorAuthService.verifyAndCompleteLogin(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Should throw when 2FA not enabled for user")
        void shouldThrowWhen2faNotEnabled() {
            testUser.setTwoFactorEnabled(false);
            TwoFactorVerifyRequest request = new TwoFactorVerifyRequest(TEMP_TOKEN, "123456");

            when(jwtService.isTwoFactorTempToken(TEMP_TOKEN)).thenReturn(true);
            when(jwtService.getUserId(TEMP_TOKEN)).thenReturn(testUser.getId());
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> twoFactorAuthService.verifyAndCompleteLogin(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("2FA is not enabled");
        }
    }
}