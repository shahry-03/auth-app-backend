package com.auth_app_backend.services.impl;

import com.auth_app_backend.dto.request.LoginRequest;
import com.auth_app_backend.dto.request.RegisterRequest;
import com.auth_app_backend.dto.response.TokenResponse;
import com.auth_app_backend.dto.response.UserResponse;
import com.auth_app_backend.entity.RefreshToken;
import com.auth_app_backend.entity.Role;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.entity.Provider;
import com.auth_app_backend.entity.VerificationToken;
import com.auth_app_backend.exception.ResourceNotFoundException;
import com.auth_app_backend.mapper.UserMapper;
import com.auth_app_backend.repositories.RoleRepository;
import com.auth_app_backend.repositories.UserRepository;
import com.auth_app_backend.security.JwtService;
import com.auth_app_backend.services.AuthService;
import com.auth_app_backend.services.RefreshTokenService;
import com.auth_app_backend.services.AccountLockoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.auth_app_backend.exception.EmailNotVerifiedException;
import com.auth_app_backend.services.EmailService;
import com.auth_app_backend.services.VerificationTokenService;
import com.auth_app_backend.dto.request.ForgotPasswordRequest;
import com.auth_app_backend.dto.request.ResetPasswordRequest;


import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final VerificationTokenService verificationTokenService;
    private final EmailService emailService;
    private final AccountLockoutService accountLockoutService;

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
                "Default USER role not found. Check Flyway V2 seed."));

        // 3. Build + save user (unverified)
        User user = User.builder()
            .email(request.email())
            .name(request.name())
            .password(passwordEncoder.encode(request.password()))
            .enabled(true)
            .emailVerified(false)                    // ← unverified
            .roles(new HashSet<>(Set.of(defaultRole)))
            .build();

        User savedUser = userRepository.save(user);

        // 4. ⚡ Generate email verification token
        VerificationToken vt = verificationTokenService
            .createEmailVerificationToken(savedUser);

        // 5. ⚡ Send verification email (async, non-blocking)
        emailService.sendVerificationEmail(
            savedUser.getEmail(),
            savedUser.getName(),
            vt.getToken()
        );

        // 6. Map to response
        return UserMapper.toResponse(savedUser);
    }

    // ============================================================
    //  LOGIN
    // ============================================================
    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {

        // 1. ⚡ Load user + CHECK ACCOUNT LOCK (before anything else)
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        accountLockoutService.checkAccountLocked(user);

        // 2. Authenticate via Spring Security (password check)
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.email(), request.password()));
        } catch (Exception e) {
            // ⚡ RECORD FAILED ATTEMPT
            boolean justLocked = accountLockoutService.recordFailedAttempt(user);

            if (justLocked) {
                log.warn("Account locked after failed attempt for user: {}", user.getId());
                // Optional: send lock notification email
                // emailService.sendAccountLockedEmail(user.getEmail(), user.getName(), 5, 15);
            }
            throw new BadCredentialsException("Invalid email or password");
        }

        // 3. ⚡ RESET FAILED COUNTER (successful password)
        accountLockoutService.resetFailedAttempts(user);

        // 4. STRICT: Email verification check
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException(
                "Please verify your email address. Check your inbox for the verification link.");
        }

        // 5. Account enabled check (admin-level disable)
        if (!user.isEnabled()) {
            throw new DisabledException("User is disabled");
        }

        // 6. ⚡ 2FA check — if enabled, return temp token instead of full tokens
        if (user.isTwoFactorEnabled()) {
            String tempToken = jwtService.generateTwoFactorTempToken(user);
            return TokenResponse.requiresTwoFactor(tempToken);
        }

        // 7. Create refresh token (persisted with jti)
        RefreshToken refreshToken = refreshTokenService.createForUser(user);

        // 8. Generate JWT access + refresh tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken(user, refreshToken.getJti());

        // 9. Build response
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

    //==========================================================
    // Email Verification 
    //==========================================================
    @Override
    @Transactional
    public void verifyEmail(String tokenValue) {
        VerificationToken token = verificationTokenService.validateToken(
            tokenValue, VerificationToken.TokenType.EMAIL_VERIFICATION);

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.saveAndFlush(user);

        verificationTokenService.markUsed(token);
    }
    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        // Don't reveal if email exists (security)
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isEmailVerified()) return;  // already verified — skip

            VerificationToken vt = verificationTokenService.createEmailVerificationToken(user);
            // emailService.sendVerificationEmail(user.getEmail(), user.getName(), vt.getToken());
        });
    }


    // ═══════════════════════════════════════════════════════════════
    //  PASSWORD RESET
    // ═══════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {

        // 1. Find user — but NEVER reveal if exists (security)
        userRepository.findByEmail(request.email()).ifPresent(user -> {

            // 2. Skip if user is OAuth-only (no password to reset)
            if (user.getProvider() != Provider.LOCAL && user.getPassword() == null) {
                log.warn("Password reset requested for OAuth user: {}", request.email());
                return;
            }

            // 3. Generate reset token (30 min expiry)
            VerificationToken token = verificationTokenService
                .createPasswordResetToken(user);

            // 4. Send reset email (async)
            emailService.sendPasswordResetEmail(
                user.getEmail(),
                user.getName(),
                token.getToken()
            );

            log.info("Password reset email sent for: {}", request.email());
        });

        // 5. If email not found — silently ignore (no info leak)
        // Response will be same for both cases
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        // 1. Validate token (checks used + expiry)
        VerificationToken token = verificationTokenService.validateToken(
            request.token(),
            VerificationToken.TokenType.PASSWORD_RESET
        );

        // 2. Get user
        User user = token.getUser();

        // 3. Encode + update password
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.saveAndFlush(user);

        // 4. Mark token as used
        verificationTokenService.markUsed(token);

        // ⚡ YEH MISSING HAI — ADD KARO
        accountLockoutService.resetFailedAttempts(user);

        // 5. SECURITY: Revoke ALL refresh tokens (force re-login everywhere)
        refreshTokenService.revokeAllForUser(user.getId());

        // 6. Send confirmation email (async)
        emailService.sendPasswordChangedEmail(user.getEmail(), user.getName());

        log.info("Password reset successful for: {}", user.getEmail());
    }


}