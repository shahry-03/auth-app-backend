package com.auth_app_backend.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import com.auth_app_backend.config.UniversalAuthProperties;

@Service
@Getter
public class CookieService {

    private final String refreshTokenCookieName;
    private final boolean cookieSecure;
    private final boolean cookieHttpOnly;
    private final String cookieDomain;
    private final String cookieSameSite;

    public CookieService(UniversalAuthProperties properties) {
        this.refreshTokenCookieName = properties.getCookie().getRefreshTokenName();
        this.cookieSecure = properties.getCookie().isSecure();
        this.cookieHttpOnly = properties.getCookie().isHttpOnly();
        this.cookieDomain = properties.getCookie().getDomain();
        this.cookieSameSite = properties.getCookie().getSameSite();
    }

    // create method to attach cookie to response
    public void attachRefreshCookie(HttpServletResponse response, String value,
            int maxAge) {
        var responseCookieBuilder = ResponseCookie.from(refreshTokenCookieName,
                value)
                .httpOnly(cookieHttpOnly)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAge)
                .sameSite(cookieSameSite);

        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            responseCookieBuilder.domain(cookieDomain);
        }
        ResponseCookie responseCookie = responseCookieBuilder.build();
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
    }

    // clear or refresh cookie
    public void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshTokenCookieName, "")
                .maxAge(0)
                .httpOnly(cookieHttpOnly)
                .path("/")
                .sameSite(cookieSameSite)
                .secure(cookieSecure);

        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            builder.domain(cookieDomain);
        }

        ResponseCookie responseCookie = builder.build();
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
    }

    public void addNoStoreHeaders(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("Pragma", "no-cache");
    }

}
