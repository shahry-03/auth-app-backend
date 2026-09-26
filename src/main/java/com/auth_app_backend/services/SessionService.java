package com.auth_app_backend.services;

import com.auth_app_backend.dto.response.SessionResponse;

import java.util.List;
import java.util.UUID;

public interface SessionService {

    /**
     * List all active sessions for a user.
     * @param currentJti jti of the current request's access token
     *                   (used to mark "current: true")
     */
    List<SessionResponse> listSessions(UUID userId, String currentJti);

    /**
     * Revoke a specific session.
     * @throws IllegalArgumentException if session not found or doesn't belong to user
     */
    void revokeSession(UUID userId, UUID sessionId, String currentJti);

    /**
     * Revoke all sessions except the current one.
     * Returns count of revoked sessions.
     */
    int revokeAllExceptCurrent(UUID userId, String currentJti);
}