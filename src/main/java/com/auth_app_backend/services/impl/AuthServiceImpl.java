package com.auth_app_backend.services.impl;

import com.auth_app_backend.dto.request.LoginRequest;
import com.auth_app_backend.dto.request.RegisterRequest;
import com.auth_app_backend.dto.response.TokenResponse;
import com.auth_app_backend.dto.response.UserResponse;
import com.auth_app_backend.entity.RefreshToken;
import com.auth_app_backend.entity.Role;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.exception.ResourceNotFoundException;
import com.auth_app_backend.mapper.UserMapper;
import com.auth_app_backend.repositories.RoleRepository;
import com.auth_app_backend.repositories.UserRepository;
import com.auth_app_backend.security.JwtService;
import com.auth_app_backend.services.AuthService;
import com.auth_app_backend.services.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    // ============================================================
    //  REGISTER
    // ============================================================
    @Override
    @Transactional
    public UserResponse registerUser(RegisterRequest request) {

        // 1. Email uniqueness
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        // 2. Default USER role
        Role defaultRole = roleRepository.findByRoleName("USER")
            .orElseThrow(() -> new ResourceNotFoundException(
                "Default USER role not found. Check RbacDataInitializer."));

        // 3. Build + save user
        User user = User.builder()
            .email(request.email())
            .name(request.name())
            .password(passwordEncoder.encode(request.password()))
            .enabled(true)
            .roles(new HashSet<>(Set.of(defaultRole)))
            .build();

        User savedUser = userRepository.save(user);

        // 4. Map to response
        return UserMapper.toResponse(savedUser);
    }

    // ============================================================
    //  LOGIN
    // ============================================================
    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {

        // 1. Authenticate via Spring Security
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.email(), request.password()));
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid email or password");
        }

        // 2. Load user
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new DisabledException("User is disabled");
        }

        // 3. Create refresh token (persisted with jti)
        RefreshToken refreshToken = refreshTokenService.createForUser(user);

        // 4. Generate JWT access + refresh tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken(user, refreshToken.getJti());

        // 5. Build response
        return TokenResponse.of(
            accessToken,
            refreshTokenValue,
            jwtService.getJwtExpirationInMillis(),
            UserMapper.toResponse(user));
    }

    // ============================================================
    //  REFRESH
    // ============================================================
    @Override
    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {

        // 1. Validate + fetch stored token
        RefreshToken oldToken = refreshTokenService.validateAndGet(refreshTokenValue);

        // 2. Rotate — revoke old, create new
        RefreshToken newToken = refreshTokenService.rotate(oldToken);

        User user = newToken.getUser();

        // 3. Generate new JWTs
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshTokenValue = jwtService.generateRefreshToken(user, newToken.getJti());

        return TokenResponse.of(
            newAccessToken,
            newRefreshTokenValue,
            jwtService.getJwtExpirationInMillis(),
            UserMapper.toResponse(user));
    }

    // ============================================================
    //  LOGOUT
    // ============================================================
    @Override
    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue != null && !refreshTokenValue.isBlank()) {
            refreshTokenService.revokeByValue(refreshTokenValue);
        }
    }
}