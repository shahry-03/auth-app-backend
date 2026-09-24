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

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Override
    @Transactional
    public RefreshToken createForUser(User user) {
        String jti = UUID.randomUUID().toString();
        long expiryMillis = jwtService.getRefreshExpirationInMillis();

        RefreshToken token = RefreshToken.builder()
            .jti(jti)
            .user(user)
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusMillis(expiryMillis))
            .revoked(false)
            .build();

        return refreshTokenRepository.save(token);
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