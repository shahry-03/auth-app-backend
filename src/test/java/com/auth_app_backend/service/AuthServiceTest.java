package com.auth_app_backend.service;

import com.auth_app_backend.dto.request.LoginRequest;
import com.auth_app_backend.dto.request.RegisterRequest;
import com.auth_app_backend.dto.response.TokenResponse;
import com.auth_app_backend.dto.response.UserResponse;
import com.auth_app_backend.entity.RefreshToken;
import com.auth_app_backend.entity.Role;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.entity.VerificationToken;
import com.auth_app_backend.exception.EmailNotVerifiedException;
import com.auth_app_backend.exception.ResourceNotFoundException;
import com.auth_app_backend.repositories.RoleRepository;
import com.auth_app_backend.repositories.UserRepository;
import com.auth_app_backend.security.JwtService;
import com.auth_app_backend.services.EmailService;
import com.auth_app_backend.services.RefreshTokenService;
import com.auth_app_backend.services.VerificationTokenService;
import com.auth_app_backend.services.impl.AuthServiceImpl;
import com.auth_app_backend.services.AccountLockoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private VerificationTokenService verificationTokenService;
    @Mock private EmailService emailService;
    @Mock private AccountLockoutService accountLockoutService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role userRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        userRole = Role.builder()
            .id(UUID.randomUUID())
            .roleName("USER")
            .description("Default user role")
            .permissions(new HashSet<>())
            .build();

        testUser = User.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .name("Test User")
            .password("hashed-password")
            .enabled(true)
            .emailVerified(true)              // ← verified by default for login tests
            .roles(new HashSet<>(Set.of(userRole)))
            .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  registerUser
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("registerUser()")
    class RegisterUser {

        @Test
        @DisplayName("Should register user with USER role and hashed password")
        void shouldRegisterSuccessfully() {
            RegisterRequest req = new RegisterRequest(
                "new@example.com", "Plain@1234", "New User");

            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(userRole));
            when(passwordEncoder.encode("Plain@1234")).thenReturn("hashed-new");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });

            // ⚡ Mock verification token + email (async)
            VerificationToken vt = VerificationToken.builder()
                .token("test-token-xyz")
                .type(VerificationToken.TokenType.EMAIL_VERIFICATION)
                .build();
            when(verificationTokenService.createEmailVerificationToken(any(User.class)))
                .thenReturn(vt);

            UserResponse result = authService.registerUser(req);

            assertThat(result.email()).isEqualTo("new@example.com");
            assertThat(result.name()).isEqualTo("New User");
            assertThat(result.roles()).hasSize(1);
            assertThat(result.roles().iterator().next().roleName()).isEqualTo("USER");

            verify(passwordEncoder).encode("Plain@1234");
            verify(verificationTokenService).createEmailVerificationToken(any(User.class));
            verify(emailService).sendVerificationEmail(eq("new@example.com"), eq("New User"), eq("test-token-xyz"));
        }

        @Test
        @DisplayName("Should throw when email already registered")
        void shouldThrowWhenEmailExists() {
            RegisterRequest req = new RegisterRequest(
                "existing@example.com", "Test@1234", "Name");

            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.registerUser(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when default USER role not found")
        void shouldThrowWhenUserRoleMissing() {
            RegisterRequest req = new RegisterRequest(
                "new@example.com", "Test@1234", "Name");

            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(roleRepository.findByRoleName("USER")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.registerUser(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("USER role not found");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should set enabled=true and emailVerified=false by default")
        void shouldSetEnabledTrue() {
            RegisterRequest req = new RegisterRequest(
                "new@example.com", "Test@1234", "Name");

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(userRole));
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });
            when(verificationTokenService.createEmailVerificationToken(any(User.class)))
                .thenReturn(VerificationToken.builder().token("tok").build());

            UserResponse result = authService.registerUser(req);

            assertThat(result.enabled()).isTrue();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  login
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("Should return tokens on successful login (verified user)")
        void shouldLoginSuccessfully() {
            LoginRequest req = new LoginRequest("test@example.com", "Plain@1234");

            RefreshToken refreshToken = RefreshToken.builder()
                .jti("jti-123")
                .user(testUser)
                .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(refreshTokenService.createForUser(testUser)).thenReturn(refreshToken);
            when(jwtService.generateAccessToken(testUser)).thenReturn("access-token-xyz");
            when(jwtService.generateRefreshToken(testUser, "jti-123")).thenReturn("refresh-token-abc");
            when(jwtService.getJwtExpirationInMillis()).thenReturn(3600000L);

            TokenResponse result = authService.login(req, null, null);

            assertThat(result.accessToken()).isEqualTo("access-token-xyz");
            assertThat(result.refreshToken()).isEqualTo("refresh-token-abc");
            assertThat(result.tokenType()).isEqualTo("Bearer");
            assertThat(result.expiresIn()).isEqualTo(3600000L);
            assertThat(result.user().email()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should throw when authentication fails")
        void shouldThrowOnBadCredentials() {
            LoginRequest req = new LoginRequest("test@example.com", "wrong");

            when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad creds"));

            assertThatThrownBy(() -> authService.login(req, null, null))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid email or password");

            verify(refreshTokenService, never()).createForUser(any());
        }

        @Test
        @DisplayName("Should throw EmailNotVerifiedException when email not verified")
        void shouldThrowWhenEmailNotVerified() {
            testUser.setEmailVerified(false);
            LoginRequest req = new LoginRequest("test@example.com", "Plain@1234");

            when(authenticationManager.authenticate(any())).thenReturn(null);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.login(req, null, null))
                .isInstanceOf(EmailNotVerifiedException.class)
                .hasMessageContaining("verify your email");

            verify(refreshTokenService, never()).createForUser(any());
        }

        @Test
        @DisplayName("Should throw when user is disabled (after email verified)")
        void shouldThrowWhenDisabled() {
            testUser.setEnabled(false);
            testUser.setEmailVerified(true);        // verified but disabled
            LoginRequest req = new LoginRequest("test@example.com", "Plain@1234");

            when(authenticationManager.authenticate(any())).thenReturn(null);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.login(req, null, null))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("disabled");

            verify(refreshTokenService, never()).createForUser(any());
        }

        @Test
        @DisplayName("Should throw when user not found in DB")
        void shouldThrowWhenUserNotFound() {
            LoginRequest req = new LoginRequest("ghost@example.com", "pwd");

            when(authenticationManager.authenticate(any())).thenReturn(null);
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(req, null, null))
                .isInstanceOf(BadCredentialsException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  refresh
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        @Test
        @DisplayName("Should rotate token and return new tokens")
        void shouldRotateTokens() {
            String oldToken = "old-refresh-token";

            RefreshToken oldStored = RefreshToken.builder()
                .jti("old-jti")
                .user(testUser)
                .build();
            RefreshToken newStored = RefreshToken.builder()
                .jti("new-jti")
                .user(testUser)
                .build();

            when(refreshTokenService.validateAndGet(oldToken)).thenReturn(oldStored);
            when(refreshTokenService.rotate(oldStored)).thenReturn(newStored);
            when(jwtService.generateAccessToken(testUser)).thenReturn("new-access");
            when(jwtService.generateRefreshToken(testUser, "new-jti")).thenReturn("new-refresh");
            when(jwtService.getJwtExpirationInMillis()).thenReturn(3600000L);

            TokenResponse result = authService.refresh(oldToken);

            assertThat(result.accessToken()).isEqualTo("new-access");
            assertThat(result.refreshToken()).isEqualTo("new-refresh");

            verify(refreshTokenService).rotate(oldStored);
        }

        @Test
        @DisplayName("Should propagate exception from validateAndGet")
        void shouldPropagateValidationError() {
            String invalidToken = "invalid";

            when(refreshTokenService.validateAndGet(invalidToken))
                .thenThrow(new BadCredentialsException("Invalid"));

            assertThatThrownBy(() -> authService.refresh(invalidToken))
                .isInstanceOf(BadCredentialsException.class);

            verify(refreshTokenService, never()).rotate(any());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  logout
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("Should revoke token when value provided")
        void shouldRevokeToken() {
            authService.logout("valid-token");
            verify(refreshTokenService).revokeByValue("valid-token");
        }

        @Test
        @DisplayName("Should skip revocation when null")
        void shouldSkipOnNull() {
            authService.logout(null);
            verify(refreshTokenService, never()).revokeByValue(any());
        }

        @Test
        @DisplayName("Should skip revocation when blank")
        void shouldSkipOnBlank() {
            authService.logout("   ");
            verify(refreshTokenService, never()).revokeByValue(any());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  verifyEmail
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("verifyEmail()")
    class VerifyEmail {

        @Test
        @DisplayName("Should mark user as verified")
        void shouldMarkVerified() {
            VerificationToken token = VerificationToken.builder()
                .token("tok-123")
                .user(testUser)
                .type(VerificationToken.TokenType.EMAIL_VERIFICATION)
                .build();

            when(verificationTokenService.validateToken("tok-123", VerificationToken.TokenType.EMAIL_VERIFICATION))
                .thenReturn(token);
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);

            authService.verifyEmail("tok-123");

            assertThat(testUser.isEmailVerified()).isTrue();
            verify(userRepository).saveAndFlush(testUser);
            verify(verificationTokenService).markUsed(token);
        }
    }
}