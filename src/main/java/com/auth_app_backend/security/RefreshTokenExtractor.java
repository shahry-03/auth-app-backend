package com.auth_app_backend.security;

import com.auth_app_backend.dto.request.RefreshTokenRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

/**
 * Extracts refresh token from HTTP request.
 * Priority: cookie → body → X-Refresh-Token header → Authorization header.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenExtractor {

    private final CookieService cookieService;
    private final JwtService jwtService;

    public Optional<String> extract(RefreshTokenRequest body, HttpServletRequest request) {
        return extractFromCookie(request)
            .or(() -> extractFromBody(body))
            .or(() -> extractFromCustomHeader(request))
            .or(() -> extractFromAuthorizationHeader(request));
    }

    private Optional<String> extractFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();

        return Arrays.stream(request.getCookies())
            .filter(c -> cookieService.getRefreshTokenCookieName().equals(c.getName()))
            .map(Cookie::getValue)
            .filter(v -> !v.isBlank())
            .findFirst();
    }

    private Optional<String> extractFromBody(RefreshTokenRequest body) {
        if (body == null || body.refreshToken() == null || body.refreshToken().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(body.refreshToken());
    }

    private Optional<String> extractFromCustomHeader(HttpServletRequest request) {
        String header = request.getHeader("X-Refresh-Token");
        if (header != null && !header.isBlank()) {
            return Optional.of(header.trim());
        }
        return Optional.empty();
    }

    private Optional<String> extractFromAuthorizationHeader(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return Optional.empty();
        }

        String candidate = authHeader.substring(7).trim();
        if (candidate.isEmpty()) return Optional.empty();

        try {
            if (jwtService.isRefreshToken(candidate)) {
                return Optional.of(candidate);
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }
}