package com.auth_app_backend.services.impl;

import com.auth_app_backend.dto.request.ChangePasswordRequest;
import com.auth_app_backend.dto.request.UpdateUserRequest;
import com.auth_app_backend.dto.response.UserResponse;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.exception.ResourceNotFoundException;
import com.auth_app_backend.mapper.UserMapper;
import com.auth_app_backend.repositories.UserRepository;
import com.auth_app_backend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.auth_app_backend.services.RefreshTokenService;
import com.auth_app_backend.services.AccountLockoutService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AccountLockoutService accountLockoutService;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
            .stream()
            .map(UserMapper::toResponse)
            .toList();
    }

    //Update User
    @Override
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Partial update — only non-null fields
        if (request.name() != null) user.setName(request.name());
        if (request.image() != null) user.setImage(request.image());
        if (request.enabled() != null) user.setEnabled(request.enabled());

        User updated = userRepository.saveAndFlush(user);
        return UserMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        userRepository.deleteById(userId);
    }

    // Change Password
    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // 1. Verify current password
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // 2. Encode and save new password
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.saveAndFlush(user);

        // ⚡ Reset failed login counter (user proved identity by changing password)
        accountLockoutService.resetFailedAttempts(user);

        // SECURITY: Revoke all refresh tokens
        refreshTokenService.revokeAllForUser(userId);
    }

    //Enable User
    @Override
    @Transactional
    public void enableUser(UUID userId, boolean enabled) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setEnabled(enabled);
        userRepository.saveAndFlush(user);
    }


    @Override
    @Transactional
    public void unlockUser(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (user.getLockedUntil() == null) {
            throw new IllegalStateException("Account is not locked");
        }

        accountLockoutService.unlockAccount(user);
    }
}