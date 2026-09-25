package com.auth_app_backend.services;

import com.auth_app_backend.dto.request.TwoFactorDisableRequest;
import com.auth_app_backend.dto.request.TwoFactorEnableRequest;
import com.auth_app_backend.dto.request.TwoFactorVerifyRequest;
import com.auth_app_backend.dto.response.TokenResponse;
import com.auth_app_backend.dto.response.TwoFactorEnableResponse;
import com.auth_app_backend.dto.response.TwoFactorSetupResponse;
import com.auth_app_backend.dto.response.TwoFactorStatusResponse;
import com.auth_app_backend.entity.User;

public interface TwoFactorAuthService {

    /**
     * Generate new TOTP secret + QR code for the user.
     * Does NOT enable 2FA yet — user must confirm with /enable.
     */
    TwoFactorSetupResponse setup(User user);

    /**
     * Verify TOTP code and enable 2FA.
     * Generates backup codes.
     */
    TwoFactorEnableResponse enable(User user, TwoFactorEnableRequest request);

    /**
     * Disable 2FA (requires password + current TOTP code).
     */
    void disable(User user, TwoFactorDisableRequest request);

    /**
     * Get current 2FA status.
     */
    TwoFactorStatusResponse getStatus(User user);

    /**
     * Step 2 of login: verify TOTP or backup code, return full tokens.
     */
    TokenResponse verifyAndCompleteLogin(TwoFactorVerifyRequest request);
}