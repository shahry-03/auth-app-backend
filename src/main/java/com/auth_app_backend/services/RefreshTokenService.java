package com.auth_app_backend.services;

import com.auth_app_backend.entity.RefreshToken;
import com.auth_app_backend.entity.User;
import java.util.UUID;

public interface RefreshTokenService {

    /**
     * Create + persist a new refresh token for the user.
     */
    RefreshToken createForUser(User user);

    /**
     * Validate incoming refresh token string.
     * Returns the stored token if valid.
     * Throws BadCredentialsException if invalid/expired/revoked.
     */
    RefreshToken validateAndGet(String refreshToken);

    /**
     * Rotate: revoke old token, create new one.
     * Returns newly created refresh token.
     */
    RefreshToken rotate(RefreshToken oldToken);

    /**
     * Revoke a token by its jti.
     */
    void revokeByJti(String jti);

    /**
     * Revoke a token by its string value.
     * Silent if token is invalid.
     */
    void revokeByValue(String refreshToken);

    void revokeAllForUser(UUID userId);
}