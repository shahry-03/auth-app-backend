package com.auth_app_backend.services.impl;

import com.auth_app_backend.entity.RefreshToken;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.repositories.RefreshTokenRepository;
import com.auth_app_backend.security.JwtService;
import com.auth_app_backend.services.RefreshTokenService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;


    @Override
    @Transactional
    public RefreshToken createForUser(User user, String ipAddress, String userAgent) {
        String jti = UUID.randomUUID().toString();
        long expiryMillis = jwtService.getRefreshExpirationInMillis();
        Instant now = Instant.now();

        RefreshToken token = RefreshToken.builder()
            .jti(jti)
            .user(user)
            .createdAt(now)
            .expiresAt(now.plusMillis(expiryMillis))
            .revoked(false)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .lastUsedAt(now)
            .build();

        return refreshTokenRepository.save(token);
    }

    // ─────────────────────────────────────────────
    //  Keep old method for internal/test use
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public RefreshToken createForUser(User user) {
        return createForUser(user, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefreshToken> getActiveSessionsForUser(UUID userId) {
        Instant now = Instant.now();
        return refreshTokenRepository.findByUserIdAndRevokedFalseAndExpiresAtAfter(userId, now);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> getSessionForUser(UUID sessionId, UUID userId) {
        return refreshTokenRepository.findById(sessionId)
            .filter(rt -> rt.getUser().getId().equals(userId));
    }

    @Override
    @Transactional
    public void revokeSession(RefreshToken session) {
        session.setRevoked(true);
        refreshTokenRepository.save(session);
        log.info("Session revoked: jti={} user={}", session.getJti(), session.getUser().getId());
    }

    @Override
    @Transactional
    public int revokeAllExcept(UUID userId, String currentJti) {
        List<RefreshToken> active = getActiveSessionsForUser(userId);
        int count = 0;

        for (RefreshToken token : active) {
            if (!token.getJti().equals(currentJti)) {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
                count++;
            }
        }

        log.info("Revoked {} other sessions for user: {}", count, userId);
        return count;
    }

    @Override
    @Transactional
    public void touchSession(RefreshToken token) {
        token.setLastUsedAt(Instant.now());
        refreshTokenRepository.save(token);
    }




    

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validateAndGet(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token type");
        }

        String jti = jwtService.getJtiFromToken(refreshToken);
        UUID userId = jwtService.getUserId(refreshToken);

        RefreshToken stored = refreshTokenRepository.findByJti(jti)
            .orElseThrow(() -> new BadCredentialsException("Refresh token not recognized"));

        if (stored.isRevoked()) {
            throw new BadCredentialsException("Refresh token revoked");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired");
        }
        if (!stored.getUser().getId().equals(userId)) {
            throw new BadCredentialsException("Refresh token does not belong to this user");
        }

        return stored;
    }

    @Override
    @Transactional
    public RefreshToken rotate(RefreshToken oldToken) {
        // 1. Revoke old
        oldToken.setRevoked(true);

        // 2. Create new
        RefreshToken newToken = createForUser(oldToken.getUser());

        // 3. Link old → new
        oldToken.setReplacedByToken(newToken.getJti());
        refreshTokenRepository.save(oldToken);

        return newToken;
    }

    @Override
    @Transactional
    public void revokeByJti(String jti) {
        refreshTokenRepository.findByJti(jti).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Override
    @Transactional
    public void revokeByValue(String refreshToken) {
        try {
            if (!jwtService.isRefreshToken(refreshToken)) return;
            revokeByJti(jwtService.getJtiFromToken(refreshToken));
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Ignoring invalid token during revoke: {}", ex.getMessage());
        }
    }


    @Override
    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}