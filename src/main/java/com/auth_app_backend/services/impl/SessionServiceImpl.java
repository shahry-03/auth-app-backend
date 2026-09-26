package com.auth_app_backend.services.impl;

import com.auth_app_backend.dto.response.SessionResponse;
import com.auth_app_backend.entity.RefreshToken;
import com.auth_app_backend.services.RefreshTokenService;
import com.auth_app_backend.services.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> listSessions(UUID userId, String currentJti) {
        List<RefreshToken> sessions = refreshTokenService.getActiveSessionsForUser(userId);

        return sessions.stream()
            .map(rt -> toResponse(rt, currentJti))
            .sorted(Comparator.comparing(SessionResponse::createdAt).reversed())
            .toList();
    }

    @Override
    @Transactional
    public void revokeSession(UUID userId, UUID sessionId, String currentJti) {
        RefreshToken session = refreshTokenService.getSessionForUser(sessionId, userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Session not found or doesn't belong to you"));

        if (session.isRevoked()) {
            throw new IllegalArgumentException("Session is already revoked");
        }

        refreshTokenService.revokeSession(session);
        log.info("Session {} revoked by user {}", sessionId, userId);
    }

    @Override
    @Transactional
    public int revokeAllExceptCurrent(UUID userId, String currentJti) {
        int count = refreshTokenService.revokeAllExcept(userId, currentJti);
        log.info("Revoked {} other sessions for user {}", count, userId);
        return count;
    }

    private SessionResponse toResponse(RefreshToken rt, String currentJti) {
        return new SessionResponse(
            rt.getId(),
            rt.getCreatedAt(),
            rt.getExpiresAt(),
            rt.getLastUsedAt(),
            rt.getIpAddress(),
            rt.getUserAgent(),
            rt.getJti().equals(currentJti)
        );
    }
}