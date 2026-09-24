package com.auth_app_backend.services;

import com.auth_app_backend.entity.User;
import com.auth_app_backend.entity.VerificationToken;

public interface VerificationTokenService {

    VerificationToken createEmailVerificationToken(User user);

    VerificationToken createPasswordResetToken(User user);

    VerificationToken validateToken(String token, VerificationToken.TokenType type);

    void markUsed(VerificationToken token);
}