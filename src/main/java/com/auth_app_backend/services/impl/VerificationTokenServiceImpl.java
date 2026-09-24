package com.auth_app_backend.services.impl;

import com.auth_app_backend.entity.User;
import com.auth_app_backend.entity.VerificationToken;
import com.auth_app_backend.repositories.VerificationTokenRepository;
import com.auth_app_backend.services.VerificationTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationTokenServiceImpl implements VerificationTokenService {

    private final VerificationTokenRepository tokenRepository;

    @Override
    @Transactional
    public VerificationToken createEmailVerificationToken(User user) {
        tokenRepository.deleteByUserAndType(user.getId(),
            VerificationToken.TokenType.EMAIL_VERIFICATION);

        VerificationToken token = VerificationToken.builder()
            .token(UUID.randomUUID().toString())
            .user(user)
            .type(VerificationToken.TokenType.EMAIL_VERIFICATION)
            .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
            .used(false)
            .build();

        return tokenRepository.save(token);
    }

    @Override
    @Transactional
    public VerificationToken createPasswordResetToken(User user) {
        tokenRepository.deleteByUserAndType(user.getId(),
            VerificationToken.TokenType.PASSWORD_RESET);

        VerificationToken token = VerificationToken.builder()
            .token(UUID.randomUUID().toString())
            .user(user)
            .type(VerificationToken.TokenType.PASSWORD_RESET)
            .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
            .used(false)
            .build();

        return tokenRepository.save(token);
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationToken validateToken(String tokenValue, VerificationToken.TokenType type) {
        VerificationToken token = tokenRepository.findByTokenAndType(tokenValue, type)
            .orElseThrow(() -> new BadCredentialsException("Invalid or expired token"));

        if (token.isUsed()) {
            throw new BadCredentialsException("Token already used");
        }

        if (token.isExpired()) {
            throw new BadCredentialsException("Token expired");
        }

        return token;
    }

    @Override
    @Transactional
    public void markUsed(VerificationToken token) {
        token.setUsed(true);
        tokenRepository.save(token);
    }
}