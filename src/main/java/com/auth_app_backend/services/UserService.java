package com.auth_app_backend.services;

import com.auth_app_backend.dto.request.ChangePasswordRequest;
import com.auth_app_backend.dto.request.UpdateUserRequest;
import com.auth_app_backend.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse getUserById(UUID userId);

    UserResponse getUserByEmail(String email);

    List<UserResponse> getAllUsers();

    UserResponse updateUser(UUID userId, UpdateUserRequest request);

    void deleteUser(UUID userId);

    void changePassword(UUID userId, ChangePasswordRequest request);

    void enableUser(UUID userId, boolean enabled);

    void unlockUser(UUID userId);
}