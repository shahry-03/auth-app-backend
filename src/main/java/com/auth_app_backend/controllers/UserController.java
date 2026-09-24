package com.auth_app_backend.controllers;

import com.auth_app_backend.dto.request.ChangePasswordRequest;
import com.auth_app_backend.dto.request.UpdateUserRequest;
import com.auth_app_backend.dto.response.ApiResponse;
import com.auth_app_backend.dto.response.UserResponse;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.mapper.UserMapper;
import com.auth_app_backend.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get current logged-in user's profile.
     * Uses @AuthenticationPrincipal User directly — no DB call needed.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(UserMapper.toResponse(user)));
    }

    /**
     * Update own profile.
     * Requires 'profile:write' permission.
     */
    @PutMapping("/me")
    @PreAuthorize("hasAuthority('profile:write')")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @AuthenticationPrincipal User user,                       // ← User, not UserDetails
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse updated = userService.updateUser(user.getId(), request);   // ← user.getId()
        return ResponseEntity.ok(ApiResponse.success("Profile updated", updated));
    }

    /**
     * Change own password.
     * Requires 'profile:write' permission.
     */
    @PostMapping("/me/change-password")
    @PreAuthorize("hasAuthority('profile:write')")
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(
            @AuthenticationPrincipal User user,                       // ← User, not UserDetails
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user.getId(), request);            // ← user.getId()
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
}