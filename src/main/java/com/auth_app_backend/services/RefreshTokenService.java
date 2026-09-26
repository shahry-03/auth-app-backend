package com.auth_app_backend.services;

import com.auth_app_backend.entity.RefreshToken;
import com.auth_app_backend.entity.User;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenService {

    /**
     * Create + persist a new refresh token for the user.
     */
    RefreshToken createForUser(User user, String ipAddress, String userAgent);

    /**
     * Backward-compatible — creates token without metadata.
     * Used by OAuth2 flow, 2FA flow, and tests.
     */
    RefreshToken createForUser(User user);   

    /**
     * List all active sessions (non-revoked, non-expired) for a user.
     */
    List<RefreshToken> getActiveSessionsForUser(UUID userId);

    /**
     * Get a specific session by ID, validating it belongs to the user.
     */
    Optional<RefreshToken> getSessionForUser(UUID sessionId, UUID userId);

    /**
     * Revoke a specific session.
     */
    void revokeSession(RefreshToken session);

    /**
     * Revoke all sessions for a user EXCEPT the given jti.
     * Returns count of revoked sessions.
     */
    int revokeAllExcept(UUID userId, String currentJti);

    /**
     * Update last_used_at on token refresh.
     */
    void touchSession(RefreshToken token);

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