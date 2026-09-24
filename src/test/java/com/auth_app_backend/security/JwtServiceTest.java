package com.auth_app_backend.security;

import com.auth_app_backend.config.UniversalAuthProperties;
import com.auth_app_backend.entity.Provider;
import com.auth_app_backend.entity.Role;
import com.auth_app_backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService Tests")
class JwtServiceTest {

    private static final String TEST_SECRET =
        "test-secret-key-that-is-at-least-64-bytes-long-for-testing-purposes-only-1234567890";

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        UniversalAuthProperties props = new UniversalAuthProperties();
        props.getJwt().setSecret(TEST_SECRET);
        props.getJwt().setExpiration(3600000L);              // 1 hour
        props.getJwt().setRefreshTokenExpiration(604800000L); // 7 days
        props.getJwt().setIssuer("test-issuer");

        jwtService = new JwtService(props);

        Role adminRole = Role.builder()
            .id(UUID.randomUUID())
            .roleName("ADMIN")
            .build();

        testUser = User.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .name("Test User")
            .password("hashed")
            .enabled(true)
            .provider(Provider.LOCAL)
            .roles(new HashSet<>(Set.of(adminRole)))
            .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("Should reject secret shorter than 64 bytes")
        void shouldRejectShortSecret() {
            UniversalAuthProperties props = new UniversalAuthProperties();
            props.getJwt().setSecret("too-short");

            assertThatThrownBy(() -> new JwtService(props))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("64 bytes");
        }

        @Test
        @DisplayName("Should reject null secret")
        void shouldRejectNullSecret() {
            UniversalAuthProperties props = new UniversalAuthProperties();
            props.getJwt().setSecret(null);

            assertThatThrownBy(() -> new JwtService(props))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should accept valid 64+ byte secret")
        void shouldAcceptValidSecret() {
            assertThat(jwtService).isNotNull();
            assertThat(jwtService.getJwtExpirationInMillis()).isEqualTo(3600000L);
            assertThat(jwtService.getRefreshExpirationInMillis()).isEqualTo(604800000L);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  generateAccessToken
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("generateAccessToken()")
    class GenerateAccessToken {

        @Test
        @DisplayName("Should generate non-empty token")
        void shouldGenerateToken() {
            String token = jwtService.generateAccessToken(testUser);

            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3); // JWT format: header.payload.signature
        }

        @Test
        @DisplayName("Should set type=access claim")
        void shouldSetTypeClaim() {
            String token = jwtService.generateAccessToken(testUser);

            assertThat(jwtService.isAccessToken(token)).isTrue();
            assertThat(jwtService.isRefreshToken(token)).isFalse();
        }

        @Test
        @DisplayName("Should embed user ID as subject")
        void shouldEmbedUserId() {
            String token = jwtService.generateAccessToken(testUser);

            UUID extracted = jwtService.getUserId(token);
            assertThat(extracted).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("Should embed email claim")
        void shouldEmbedEmail() {
            String token = jwtService.generateAccessToken(testUser);

            assertThat(jwtService.getEmailFromToken(token))
                .isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should embed roles claim")
        void shouldEmbedRoles() {
            String token = jwtService.generateAccessToken(testUser);

            List<String> roles = jwtService.getRolesFromToken(token);
            assertThat(roles).containsExactly("ADMIN");
        }

        @Test
        @DisplayName("Should handle user with null roles")
        void shouldHandleNullRoles() {
            User noRoleUser = User.builder()
                .id(UUID.randomUUID())
                .email("norole@test.com")
                .roles(null)
                .build();

            String token = jwtService.generateAccessToken(noRoleUser);

            assertThat(jwtService.getRolesFromToken(token)).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  generateRefreshToken
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("generateRefreshToken()")
    class GenerateRefreshToken {

        @Test
        @DisplayName("Should generate refresh token with type=refresh")
        void shouldGenerateRefreshToken() {
            String jti = UUID.randomUUID().toString();
            String token = jwtService.generateRefreshToken(testUser, jti);

            assertThat(jwtService.isRefreshToken(token)).isTrue();
            assertThat(jwtService.isAccessToken(token)).isFalse();
        }

        @Test
        @DisplayName("Should embed provided jti")
        void shouldEmbedJti() {
            String jti = "my-custom-jti-" + UUID.randomUUID();
            String token = jwtService.generateRefreshToken(testUser, jti);

            assertThat(jwtService.getJtiFromToken(token)).isEqualTo(jti);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  parseToken / validateToken
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("parseToken & validateToken")
    class ParseAndValidate {

        @Test
        @DisplayName("validateToken — true for valid token")
        void validateValid() {
            String token = jwtService.generateAccessToken(testUser);
            assertThat(jwtService.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("validateToken — false for garbage")
        void validateGarbage() {
            assertThat(jwtService.validateToken("garbage")).isFalse();
            assertThat(jwtService.validateToken("a.b.c")).isFalse();
        }

        @Test
        @DisplayName("validateToken — false for tampered token")
        void validateTampered() {
            String token = jwtService.generateAccessToken(testUser);
            String tampered = token.substring(0, token.length() - 5) + "xxxxx";
            assertThat(jwtService.validateToken(tampered)).isFalse();
        }

        @Test
        @DisplayName("parseToken — throws on invalid signature")
        void parseThrowsOnInvalidSignature() {
            // Build second JwtService with different secret
            UniversalAuthProperties otherProps = new UniversalAuthProperties();
            otherProps.getJwt().setSecret(
                "different-secret-key-that-is-at-least-64-bytes-long-for-test-purposes-9876543210");
            JwtService otherService = new JwtService(otherProps);

            String token = jwtService.generateAccessToken(testUser);

            assertThatThrownBy(() -> otherService.parseToken(token))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid or expired token");
        }

        @Test
        @DisplayName("isAccessToken — false for non-JWT string")
        void isAccessTokenFalseForGarbage() {
            assertThat(jwtService.isAccessToken("not-a-token")).isFalse();
        }

        @Test
        @DisplayName("isRefreshToken — false for non-JWT string")
        void isRefreshTokenFalseForGarbage() {
            assertThat(jwtService.isRefreshToken("not-a-token")).isFalse();
        }
    }
}