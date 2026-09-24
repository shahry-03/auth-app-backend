package com.auth_app_backend.security;

import com.auth_app_backend.config.UniversalAuthProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CookieService Tests")
class CookieServiceTest {

    private CookieService cookieService;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        UniversalAuthProperties props = new UniversalAuthProperties();
        props.getCookie().setRefreshTokenName("refresh_token");
        props.getCookie().setSecure(false);
        props.getCookie().setHttpOnly(true);
        props.getCookie().setSameSite("Lax");
        props.getCookie().setDomain("");

        cookieService = new CookieService(props);
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("Constructor — should load properties")
    void constructorLoadsProperties() {
        assertThat(cookieService.getRefreshTokenCookieName()).isEqualTo("refresh_token");
        assertThat(cookieService.isCookieHttpOnly()).isTrue();
        assertThat(cookieService.isCookieSecure()).isFalse();
        assertThat(cookieService.getCookieSameSite()).isEqualTo("Lax");
    }

    @Test
    @DisplayName("attachRefreshCookie — should set Set-Cookie header")
    void shouldAttachCookie() {
        cookieService.attachRefreshCookie(response, "test-token", 3600);

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("refresh_token=test-token");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("SameSite=Lax");
        assertThat(setCookie).contains("Path=/");
        assertThat(setCookie).contains("Max-Age=3600");
    }

    @Test
    @DisplayName("attachRefreshCookie — with domain")
    void shouldAttachCookieWithDomain() {
        UniversalAuthProperties props = new UniversalAuthProperties();
        props.getCookie().setDomain("example.com");
        CookieService svc = new CookieService(props);

        svc.attachRefreshCookie(response, "tok", 60);

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).contains("Domain=example.com");
    }

    @Test
    @DisplayName("attachRefreshCookie — empty domain omitted")
    void shouldOmitEmptyDomain() {
        cookieService.attachRefreshCookie(response, "tok", 60);

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).doesNotContain("Domain=");
    }

    @Test
    @DisplayName("clearRefreshCookie — should set Max-Age=0 and empty value")
    void shouldClearCookie() {
        cookieService.clearRefreshCookie(response);

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("refresh_token=");
        assertThat(setCookie).contains("Max-Age=0");
        assertThat(setCookie).contains("Path=/");
    }

    @Test
    @DisplayName("clearRefreshCookie — respects domain config")
    void clearRespectsDomain() {
        UniversalAuthProperties props = new UniversalAuthProperties();
        props.getCookie().setDomain("example.com");
        CookieService svc = new CookieService(props);

        svc.clearRefreshCookie(response);

        assertThat(response.getHeader("Set-Cookie")).contains("Domain=example.com");
    }

    @Test
    @DisplayName("addNoStoreHeaders — should set cache headers")
    void shouldAddNoStoreHeaders() {
        cookieService.addNoStoreHeaders(response);

        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");
    }
}