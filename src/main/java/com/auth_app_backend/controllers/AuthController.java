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
import com.auth_app_backend.dto.request.VerifyEmailRequest;
import com.auth_app_backend.dto.request.ResendVerificationRequest;
import com.auth_app_backend.dto.request.ForgotPasswordRequest;
import com.auth_app_backend.dto.request.ResetPasswordRequest;
import com.auth_app_backend.ratelimit.RateLimit;
import com.auth_app_backend.helper.RequestMetadataExtractor;
import jakarta.servlet.http.HttpServletRequest;

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
    @RateLimit(name = "register", capacity = 3, refillTokens = 3, refillPeriod = 1, refillUnit = RateLimit.RefillUnit.HOURS)
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
    @RateLimit(name = "login", capacity = 5, refillTokens = 5, refillPeriod = 15)
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        // Extract IP + User-Agent
        String ipAddress = RequestMetadataExtractor.extractIp(httpRequest);
        String userAgent = RequestMetadataExtractor.extractUserAgent(httpRequest);

        TokenResponse tokens = authService.login(request, ipAddress, userAgent);


        // ⚡ 2FA required — return temp token, no cookie yet
        if (Boolean.TRUE.equals(tokens.requiresTwoFactor())) {
            return ResponseEntity.ok(ApiResponse.success(
                "Two-factor authentication required", tokens));
        }

        // Normal login — attach refresh cookie
        attachRefreshCookie(response, tokens.refreshToken());
        cookieService.addNoStoreHeaders(response);

        return ResponseEntity.ok(ApiResponse.success("Login successful", tokens));
    }

    // ─────────────────────────────────────────────
    //  REFRESH
    // ─────────────────────────────────────────────
    @PostMapping("/refresh")
    @RateLimit(name = "refresh", capacity = 30, refillTokens = 30, refillPeriod = 1, refillUnit = RateLimit.RefillUnit.MINUTES)
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


    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.token());
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    @RateLimit(name = "resend-verification", capacity = 3, refillTokens = 3, refillPeriod = 1, refillUnit = RateLimit.RefillUnit.HOURS)
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerificationEmail(request.email());
        return ResponseEntity.ok(ApiResponse.success(
            "If the email is registered and unverified, a verification link has been sent"));
    }



    // ═══════════════════════════════════════════════════════════════
    //  PASSWORD RESET
    // ═══════════════════════════════════════════════════════════════

    /**
     * Request password reset email.
     * Always returns 200 (does not reveal if email exists).
     */
    @PostMapping("/forgot-password")
    @RateLimit(name = "forgot-password", capacity = 5, refillTokens = 5, refillPeriod = 1, refillUnit = RateLimit.RefillUnit.HOURS)
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
            "If the email is registered, a password reset link has been sent"));
    }

    /**
     * Reset password using token from email.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
            "Password reset successfully. Please login with your new password."));
    }




}