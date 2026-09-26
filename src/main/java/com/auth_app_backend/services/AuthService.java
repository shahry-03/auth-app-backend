package com.auth_app_backend.services;

import com.auth_app_backend.dto.request.LoginRequest;
import com.auth_app_backend.dto.request.RegisterRequest;
import com.auth_app_backend.dto.response.TokenResponse;
import com.auth_app_backend.dto.response.UserResponse;
import com.auth_app_backend.dto.request.ForgotPasswordRequest;
import com.auth_app_backend.dto.request.ResetPasswordRequest;

public interface AuthService {

    UserResponse registerUser(RegisterRequest request);

    TokenResponse login(LoginRequest request, String ipAddress, String userAgent);

    TokenResponse refresh(String refreshToken);

    void logout(String refreshToken);

    //Email Verification
    void verifyEmail(String token);
    void resendVerificationEmail(String email);

    // ─── Password Reset ────────────────────────
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}