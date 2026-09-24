package com.auth_app_backend.security;

import com.auth_app_backend.dto.request.RefreshTokenRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenExtractor Tests")
class RefreshTokenExtractorTest {

    @Mock private CookieService cookieService;
    @Mock private JwtService jwtService;

    @InjectMocks
    private RefreshTokenExtractor extractor;

    @BeforeEach
    void setUp() {
        // Default cookie name behavior
        lenient().when(cookieService.getRefreshTokenCookieName()).thenReturn("refresh_token");
    }

    // ═══════════════════════════════════════════════════════════
    //  COOKIE (priority 1)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Cookie Source")
    class CookieSource {

        @Test
        @DisplayName("Should extract from cookie first")
        void shouldExtractFromCookie() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new jakarta.servlet.http.Cookie("refresh_token", "cookie-token"));

            Optional<String> result = extractor.extract(null, request);

            assertThat(result).contains("cookie-token");
        }

        @Test
        @DisplayName("Should skip blank cookie value")
        void shouldSkipBlankCookie() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new jakarta.servlet.http.Cookie("refresh_token", "  "));

            Optional<String> result = extractor.extract(null, request);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should skip wrong cookie name")
        void shouldSkipWrongCookie() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new jakarta.servlet.http.Cookie("other_cookie", "value"));

            Optional<String> result = extractor.extract(null, request);

            assertThat(result).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  BODY (priority 2)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Body Source")
    class BodySource {

        @Test
        @DisplayName("Should extract from body when no cookie")
        void shouldExtractFromBody() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            RefreshTokenRequest body = new RefreshTokenRequest("body-token");

            Optional<String> result = extractor.extract(body, request);

            assertThat(result).contains("body-token");
        }

        @Test
        @DisplayName("Should skip blank body token")
        void shouldSkipBlankBody() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            RefreshTokenRequest body = new RefreshTokenRequest("  ");

            Optional<String> result = extractor.extract(body, request);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should skip null body")
        void shouldSkipNullBody() {
            MockHttpServletRequest request = new MockHttpServletRequest();

            Optional<String> result = extractor.extract(null, request);

            assertThat(result).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  CUSTOM HEADER (priority 3)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Custom Header")
    class CustomHeader {

        @Test
        @DisplayName("Should extract from X-Refresh-Token header")
        void shouldExtractFromHeader() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Refresh-Token", "header-token");

            Optional<String> result = extractor.extract(null, request);

            assertThat(result).contains("header-token");
        }

        @Test
        @DisplayName("Should trim whitespace from header")
        void shouldTrimHeader() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Refresh-Token", "  header-token  ");

            Optional<String> result = extractor.extract(null, request);

            assertThat(result).contains("header-token");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  AUTHORIZATION HEADER (priority 4)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Authorization Header")
    class AuthHeader {

        @Test
        @DisplayName("Should extract Bearer token if it is a refresh token")
        void shouldExtractFromAuthHeader() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer refresh-token-xyz");
            when(jwtService.isRefreshToken("refresh-token-xyz")).thenReturn(true);

            Optional<String> result = extractor.extract(null, request);

            assertThat(result).contains("refresh-token-xyz");
        }

        @Test
        @DisplayName("Should reject Bearer token if not a refresh token")
        void shouldRejectNonRefreshToken() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer access-token-xyz");
            when(jwtService.isRefreshToken("access-token-xyz")).thenReturn(false);

            Optional<String> result = extractor.extract(null, request);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should ignore non-Bearer Authorization")
        void shouldIgnoreNonBearer() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

            Optional<String> result = extractor.extract(null, request);

            assertThat(result).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  PRIORITY ORDER
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("Cookie should take priority over body")
    void cookiePriorityOverBody() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("refresh_token", "from-cookie"));
        RefreshTokenRequest body = new RefreshTokenRequest("from-body");

        Optional<String> result = extractor.extract(body, request);

        assertThat(result).contains("from-cookie");
    }

    @Test
    @DisplayName("Body should take priority over header")
    void bodyPriorityOverHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Refresh-Token", "from-header");
        RefreshTokenRequest body = new RefreshTokenRequest("from-body");

        Optional<String> result = extractor.extract(body, request);

        assertThat(result).contains("from-body");
    }

    @Test
    @DisplayName("Empty request returns empty")
    void emptyRequestReturnsEmpty() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(extractor.extract(null, request)).isEmpty();
    }
}