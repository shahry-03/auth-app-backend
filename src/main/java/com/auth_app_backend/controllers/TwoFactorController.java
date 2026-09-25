package com.auth_app_backend.controllers;

import com.auth_app_backend.dto.request.TwoFactorDisableRequest;
import com.auth_app_backend.dto.request.TwoFactorEnableRequest;
import com.auth_app_backend.dto.request.TwoFactorVerifyRequest;
import com.auth_app_backend.dto.response.*;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.ratelimit.RateLimit;
import com.auth_app_backend.security.CookieService;
import com.auth_app_backend.security.JwtService;
import com.auth_app_backend.services.TwoFactorAuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private final TwoFactorAuthService twoFactorAuthService;
    private final CookieService cookieService;
    private final JwtService jwtService;

    /**
     * Step 1 of 2FA setup — generate secret + QR code.
     * Requires authentication (user must be logged in).
     */
    @PostMapping("/setup")
    public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> setup(
            @AuthenticationPrincipal User user) {
        TwoFactorSetupResponse response = twoFactorAuthService.setup(user);
        return ResponseEntity.ok(ApiResponse.success(
            "Scan the QR code with your authenticator app", response));
    }

    /**
     * Step 2 — verify code and enable 2FA.
     * Returns backup codes (shown once).
     */
    @PostMapping("/enable")
    public ResponseEntity<ApiResponse<TwoFactorEnableResponse>> enable(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TwoFactorEnableRequest request) {
        TwoFactorEnableResponse response = twoFactorAuthService.enable(user, request);
        return ResponseEntity.ok(ApiResponse.success(
            "2FA enabled. Save your backup codes in a safe place.", response));
    }

    /**
     * Disable 2FA — requires password + current TOTP code.
     */
    @PostMapping("/disable")
    public ResponseEntity<ApiResponse<Void>> disable(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TwoFactorDisableRequest request) {
        twoFactorAuthService.disable(user, request);
        return ResponseEntity.ok(ApiResponse.success(
            "2FA disabled. All sessions have been logged out."));
    }

    /**
     * Get 2FA status — enabled + remaining backup codes.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<TwoFactorStatusResponse>> status(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
            twoFactorAuthService.getStatus(user)));
    }

    /**
     * Login step 2 — verify TOTP/backup code with temp token.
     * No authentication required (uses temp token from login response).
     */
    @PostMapping("/verify")
    @RateLimit(name = "2fa-verify", capacity = 10, refillTokens = 10, refillPeriod = 15)
    public ResponseEntity<ApiResponse<TokenResponse>> verify(
            @Valid @RequestBody TwoFactorVerifyRequest request,
            HttpServletResponse response) {

        TokenResponse tokens = twoFactorAuthService.verifyAndCompleteLogin(request);

        // Attach refresh cookie
        int maxAgeSeconds = (int) (jwtService.getRefreshExpirationInMillis() / 1000);
        cookieService.attachRefreshCookie(response, tokens.refreshToken(), maxAgeSeconds);
        cookieService.addNoStoreHeaders(response);

        return ResponseEntity.ok(ApiResponse.success("Login successful", tokens));
    }
}