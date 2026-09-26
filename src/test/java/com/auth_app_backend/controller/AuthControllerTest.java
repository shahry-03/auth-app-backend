package com.auth_app_backend.controller;

import com.auth_app_backend.config.UniversalAuthProperties;
import com.auth_app_backend.controllers.AuthController;
import com.auth_app_backend.dto.response.TokenResponse;
import com.auth_app_backend.dto.response.UserResponse;
import com.auth_app_backend.security.CookieService;
import com.auth_app_backend.security.JwtAuthenticationFilter;
import com.auth_app_backend.security.JwtService;
import com.auth_app_backend.security.RefreshTokenExtractor;
import com.auth_app_backend.services.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "universal.auth.jwt.secret=test-secret-key-that-is-at-least-64-bytes-long-for-testing-purpose-only-x",
    "universal.auth.jwt.expiration=3600000",
    "universal.auth.jwt.refresh-token-expiration=604800000",
    "universal.auth.oauth2.enabled=false"
})
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private RefreshTokenExtractor refreshTokenExtractor;
    @MockBean private CookieService cookieService;
    @MockBean private JwtService jwtService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private UniversalAuthProperties properties;

    private UserResponse userResponse;
    private TokenResponse tokenResponse;

    @BeforeEach
    void setUp() {
        userResponse = new UserResponse(
            UUID.randomUUID(),
            "test@example.com",
            "Test User",
            null,
            true,
            Instant.now(),
            Instant.now(),
            null,
            java.util.Set.of()
        );

        tokenResponse = TokenResponse.of(
            "access-token-xyz",
            "refresh-token-abc",
            3600000L,
            userResponse
        );
    }

    // ═══════════════════════════════════════════════════════════
    //  POST /api/v1/auth/register
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("Should return 201 with user on valid request")
        void shouldReturn201() throws Exception {
            when(authService.registerUser(any())).thenReturn(userResponse);

            String body = """
                {
                  "email": "test@example.com",
                  "password": "Test@1234",
                  "name": "Test User"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.name").value("Test User"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Should return 400 on invalid email")
        void shouldReturn400OnInvalidEmail() throws Exception {
            String body = """
                {
                  "email": "not-an-email",
                  "password": "Test@1234",
                  "name": "Test"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());

            verify(authService, never()).registerUser(any());
        }

        @Test
        @DisplayName("Should return 400 on weak password")
        void shouldReturn400OnWeakPassword() throws Exception {
            String body = """
                {
                  "email": "test@example.com",
                  "password": "123",
                  "name": "Test"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
        }

        @Test
        @DisplayName("Should return 400 on empty body")
        void shouldReturn400OnMissingFields() throws Exception {
            String body = "{}";

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
        }

        @Test
        @DisplayName("Should return 400 on duplicate email (via IllegalArgument)")
        void shouldReturn400OnDuplicate() throws Exception {
            when(authService.registerUser(any()))
                .thenThrow(new IllegalArgumentException("Email already registered"));

            String body = """
                {
                  "email": "existing@example.com",
                  "password": "Test@1234",
                  "name": "Test"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already registered"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  POST /api/v1/auth/login
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("Should return 200 with tokens on success")
        void shouldReturn200() throws Exception {
            when(authService.login(any(), any(), any())).thenReturn(tokenResponse);
            when(jwtService.getRefreshExpirationInMillis()).thenReturn(604800000L);

            String body = """
                {
                  "email": "test@example.com",
                  "password": "Test@1234"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-xyz"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-abc"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));

            // verify cookie was attached
            verify(cookieService).attachRefreshCookie(any(), eq("refresh-token-abc"), anyInt());
            verify(cookieService).addNoStoreHeaders(any());
        }

        @Test
        @DisplayName("Should return 401 on bad credentials")
        void shouldReturn401OnBadCreds() throws Exception {
            when(authService.login(any(), any(), any()))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

            String body = """
                {
                  "email": "test@example.com",
                  "password": "wrong"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials or account unavailable"));
        }

        @Test
        @DisplayName("Should return 400 when email is blank")
        void shouldReturn400OnBlankEmail() throws Exception {
            String body = """
                {
                  "email": "",
                  "password": "Test@1234"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  POST /api/v1/auth/refresh
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("Should return 200 with new tokens")
        void shouldReturnNewTokens() throws Exception {
            when(refreshTokenExtractor.extract(any(), any(HttpServletRequest.class)))
                .thenReturn(Optional.of("valid-refresh-token"));
            when(authService.refresh("valid-refresh-token")).thenReturn(tokenResponse);
            when(jwtService.getRefreshExpirationInMillis()).thenReturn(604800000L);

            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());

            verify(cookieService).attachRefreshCookie(any(), eq("refresh-token-abc"), anyInt());
        }

        @Test
        @DisplayName("Should return 401 when refresh token missing")
        void shouldReturn401WhenMissing() throws Exception {
            when(refreshTokenExtractor.extract(any(), any(HttpServletRequest.class)))
                .thenReturn(Optional.empty());

            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials or account unavailable"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  POST /api/v1/auth/logout
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("Should return 204 and clear cookie")
        void shouldReturn204() throws Exception {
            when(refreshTokenExtractor.extract(any(), any(HttpServletRequest.class)))
                .thenReturn(Optional.of("some-token"));

            mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());

            verify(authService).logout("some-token");
            verify(cookieService).clearRefreshCookie(any());
            verify(cookieService).addNoStoreHeaders(any());
        }

        @Test
        @DisplayName("Should return 204 even without token")
        void shouldReturn204WithoutToken() throws Exception {
            when(refreshTokenExtractor.extract(any(), any(HttpServletRequest.class)))
                .thenReturn(Optional.empty());

            mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());

            verify(authService, never()).logout(anyString());
            verify(cookieService).clearRefreshCookie(any());
        }
    }
}