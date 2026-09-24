package com.auth_app_backend.service;

import com.auth_app_backend.entity.RefreshToken;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.repositories.RefreshTokenRepository;
import com.auth_app_backend.security.JwtService;
import com.auth_app_backend.services.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService Tests")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    // ═══════════════════════════════════════════════════════════
    //  Test Fixtures — Dummy objects
    // ═══════════════════════════════════════════════════════════

    private User createTestUser() {
        return User.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .name("Test User")
            .password("hashed")
            .enabled(true)
            .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  createForUser
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createForUser()")
    class CreateForUser {

        @Test
        @DisplayName("Should create refresh token with UUID jti and expiry")
        void shouldCreateTokenWithJtiAndExpiry() {
            // given
            User user = createTestUser();
            long expiryMillis = 604_800_000L; // 7 days
            when(jwtService.getRefreshExpirationInMillis()).thenReturn(expiryMillis);
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            RefreshToken result = refreshTokenService.createForUser(user);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getJti()).isNotBlank();
            assertThat(result.getUser()).isEqualTo(user);
            assertThat(result.isRevoked()).isFalse();
            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.getExpiresAt()).isAfter(Instant.now());

            // verify save was called
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());

            RefreshToken saved = captor.getValue();
            assertThat(saved.getJti()).isEqualTo(result.getJti());
        }

        @Test
        @DisplayName("Should generate unique jti for each call")
        void shouldGenerateUniqueJti() {
            // given
            User user = createTestUser();
            when(jwtService.getRefreshExpirationInMillis()).thenReturn(604_800_000L);
            when(refreshTokenRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            RefreshToken t1 = refreshTokenService.createForUser(user);
            RefreshToken t2 = refreshTokenService.createForUser(user);

            // then
            assertThat(t1.getJti()).isNotEqualTo(t2.getJti());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  validateAndGet
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("validateAndGet()")
    class ValidateAndGet {

        @Test
        @DisplayName("Should throw when token is not a refresh token")
        void shouldThrowWhenNotRefreshToken() {
            // given
            String token = "not-a-refresh-token";
            when(jwtService.isRefreshToken(token)).thenReturn(false);

            // when/then
            assertThatThrownBy(() -> refreshTokenService.validateAndGet(token))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid refresh token type");
        }

        @Test
        @DisplayName("Should throw when token not found in DB")
        void shouldThrowWhenTokenNotInDb() {
            // given
            String token = "valid-refresh-token";
            String jti = UUID.randomUUID().toString();
            UUID userId = UUID.randomUUID();

            when(jwtService.isRefreshToken(token)).thenReturn(true);
            when(jwtService.getJtiFromToken(token)).thenReturn(jti);
            when(jwtService.getUserId(token)).thenReturn(userId);
            when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> refreshTokenService.validateAndGet(token))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("not recognized");
        }

        @Test
        @DisplayName("Should throw when token is revoked")
        void shouldThrowWhenRevoked() {
            // given
            String token = "revoked-token";
            String jti = UUID.randomUUID().toString();
            UUID userId = UUID.randomUUID();

            RefreshToken stored = RefreshToken.builder()
                .jti(jti)
                .user(User.builder().id(userId).build())
                .revoked(true)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

            when(jwtService.isRefreshToken(token)).thenReturn(true);
            when(jwtService.getJtiFromToken(token)).thenReturn(jti);
            when(jwtService.getUserId(token)).thenReturn(userId);
            when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(stored));

            // when/then
            assertThatThrownBy(() -> refreshTokenService.validateAndGet(token))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("revoked");
        }

        @Test
        @DisplayName("Should throw when token is expired")
        void shouldThrowWhenExpired() {
            // given
            String token = "expired-token";
            String jti = UUID.randomUUID().toString();
            UUID userId = UUID.randomUUID();

            RefreshToken stored = RefreshToken.builder()
                .jti(jti)
                .user(User.builder().id(userId).build())
                .revoked(false)
                .expiresAt(Instant.now().minusSeconds(3600)) // 1 hour ago
                .build();

            when(jwtService.isRefreshToken(token)).thenReturn(true);
            when(jwtService.getJtiFromToken(token)).thenReturn(jti);
            when(jwtService.getUserId(token)).thenReturn(userId);
            when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(stored));

            // when/then
            assertThatThrownBy(() -> refreshTokenService.validateAndGet(token))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Should throw when token belongs to different user")
        void shouldThrowWhenUserMismatch() {
            // given
            String token = "valid-token";
            String jti = UUID.randomUUID().toString();
            UUID tokenUserId = UUID.randomUUID();
            UUID differentUserId = UUID.randomUUID();

            RefreshToken stored = RefreshToken.builder()
                .jti(jti)
                .user(User.builder().id(differentUserId).build())
                .revoked(false)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

            when(jwtService.isRefreshToken(token)).thenReturn(true);
            when(jwtService.getJtiFromToken(token)).thenReturn(jti);
            when(jwtService.getUserId(token)).thenReturn(tokenUserId);
            when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(stored));

            // when/then
            assertThatThrownBy(() -> refreshTokenService.validateAndGet(token))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("does not belong");
        }

        @Test
        @DisplayName("Should return stored token when all checks pass")
        void shouldReturnValidToken() {
            // given
            String token = "valid-token";
            String jti = UUID.randomUUID().toString();
            UUID userId = UUID.randomUUID();

            RefreshToken stored = RefreshToken.builder()
                .jti(jti)
                .user(User.builder().id(userId).build())
                .revoked(false)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

            when(jwtService.isRefreshToken(token)).thenReturn(true);
            when(jwtService.getJtiFromToken(token)).thenReturn(jti);
            when(jwtService.getUserId(token)).thenReturn(userId);
            when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(stored));

            // when
            RefreshToken result = refreshTokenService.validateAndGet(token);

            // then
            assertThat(result).isEqualTo(stored);
            assertThat(result.isRevoked()).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  revokeByJti
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("revokeByJti()")
    class RevokeByJti {

        @Test
        @DisplayName("Should mark token as revoked and save")
        void shouldRevokeToken() {
            // given
            String jti = UUID.randomUUID().toString();
            RefreshToken token = RefreshToken.builder()
                .jti(jti)
                .revoked(false)
                .build();
            when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(token));

            // when
            refreshTokenService.revokeByJti(jti);

            // then
            assertThat(token.isRevoked()).isTrue();
            verify(refreshTokenRepository).save(token);
        }

        @Test
        @DisplayName("Should do nothing if token not found")
        void shouldDoNothingIfNotFound() {
            // given
            String jti = UUID.randomUUID().toString();
            when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.empty());

            // when
            refreshTokenService.revokeByJti(jti);

            // then
            verify(refreshTokenRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  revokeByValue
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("revokeByValue()")
    class RevokeByValue {

        @Test
        @DisplayName("Should extract jti and revoke")
        void shouldExtractJtiAndRevoke() {
            // given
            String token = "some-token";
            String jti = UUID.randomUUID().toString();
            RefreshToken stored = RefreshToken.builder().jti(jti).revoked(false).build();

            when(jwtService.isRefreshToken(token)).thenReturn(true);
            when(jwtService.getJtiFromToken(token)).thenReturn(jti);
            when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(stored));

            // when
            refreshTokenService.revokeByValue(token);

            // then
            assertThat(stored.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("Should silently ignore invalid token")
        void shouldIgnoreInvalidToken() {
            // given
            String token = "invalid";
            when(jwtService.isRefreshToken(token)).thenReturn(false);

            // when - should not throw
            refreshTokenService.revokeByValue(token);

            // then
            verify(refreshTokenRepository, never()).save(any());
        }
    }
}