package com.auth_app_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response from /login.
 *
 * Two scenarios:
 * 1. Normal login: accessToken + refreshToken + user populated
 * 2. 2FA required: requiresTwoFactor=true + tempToken populated (rest null)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn,
    UserResponse user,
    Boolean requiresTwoFactor,
    String tempToken
) {
    /**
     * Normal login — full tokens.
     */
    public static TokenResponse of(String accessToken, String refreshToken,
                                   long expiresIn, UserResponse user) {
        return new TokenResponse(accessToken, refreshToken, "Bearer",
            expiresIn, user, null, null);
    }

    /**
     * 2FA required — no tokens yet, only temp token for verification step.
     */
    public static TokenResponse requiresTwoFactor(String tempToken) {
        return new TokenResponse(null, null, null, null, null, true, tempToken);
    }
}