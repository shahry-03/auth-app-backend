package com.auth_app_backend.controllers;

import com.auth_app_backend.dto.request.LoginRequest;
import com.auth_app_backend.dto.request.RefreshTokenRequest;
import com.auth_app_backend.dto.request.RegisterRequest;
import com.auth_app_backend.dto.response.ApiResponse;
import com.auth_app_backend.dto.response.TokenResponse;
import com.auth_app_backend.dto.response.UserResponse;
import com.auth_app_backend.security.CookieService;
import com.auth_app_backend.security.JwtService;
import com.auth_app_backend.security.RefreshTokenExtractor;
import com.auth_app_backend.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenExtractor refreshTokenExtractor;
    private final CookieService cookieService;
    private final JwtService jwtService;

    // ─────────────────────────────────────────────
    //  REGISTER
    // ─────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse user = authService.registerUser(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("User registered successfully", user));
    }

    // ─────────────────────────────────────────────
    //  LOGIN
    // ─────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        TokenResponse tokens = authService.login(request);
        attachRefreshCookie(response, tokens.refreshToken());
        cookieService.addNoStoreHeaders(response);

        return ResponseEntity.ok(ApiResponse.success("Login successful", tokens));
    }

    // ─────────────────────────────────────────────
    //  REFRESH
    // ─────────────────────────────────────────────
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = refreshTokenExtractor.extract(body, request)
            .orElseThrow(() -> new BadCredentialsException("Refresh token is missing"));

        TokenResponse tokens = authService.refresh(refreshToken);
        attachRefreshCookie(response, tokens.refreshToken());
        cookieService.addNoStoreHeaders(response);

        return ResponseEntity.ok(ApiResponse.success("Token refreshed", tokens));
    }

    // ─────────────────────────────────────────────
    //  LOGOUT
    // ─────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       HttpServletResponse response) {

        refreshTokenExtractor.extract(null, request)
            .ifPresent(authService::logout);

        cookieService.clearRefreshCookie(response);
        cookieService.addNoStoreHeaders(response);
        SecurityContextHolder.clearContext();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // ─────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────

    /**
     * Attach refresh token as HTTP-only cookie.
     * Cookie maxAge is in SECONDS — convert from millis.
     */
    private void attachRefreshCookie(HttpServletResponse response, String refreshToken) {
        int maxAgeSeconds = (int) (jwtService.getRefreshExpirationInMillis() / 1000);
        cookieService.attachRefreshCookie(response, refreshToken, maxAgeSeconds);
    }
}