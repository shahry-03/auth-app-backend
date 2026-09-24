package com.auth_app_backend.services;

import com.auth_app_backend.dto.request.LoginRequest;
import com.auth_app_backend.dto.request.RegisterRequest;
import com.auth_app_backend.dto.response.TokenResponse;
import com.auth_app_backend.dto.response.UserResponse;

public interface AuthService {

    UserResponse registerUser(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(String refreshToken);

    void logout(String refreshToken);
}