package com.auth_app_backend.controllers;

import com.auth_app_backend.dto.response.ApiResponse;
import com.auth_app_backend.dto.response.UserResponse;
import com.auth_app_backend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")   // class-level — saare endpoints ADMIN only
public class AdminUserController {

    private final UserService userService;

    /**
     * List all users.
     * Requires 'user:read' permission.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers()));
    }

    /**
     * Get user by ID.
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(userId)));
    }

    /**
     * Update user by ID.
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID userId,
            @RequestBody com.auth_app_backend.dto.request.UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User updated",
            userService.updateUser(userId, request)));
    }

    /**
     * Enable / disable user.
     */
    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<ApiResponse<Void>> enableUser(
            @PathVariable UUID userId,
            @RequestParam boolean enabled) {
        userService.enableUser(userId, enabled);
        return ResponseEntity.ok(ApiResponse.success(
            enabled ? "User enabled" : "User disabled"));
    }

    /**
     * Delete user.
     * Requires 'user:delete' permission.
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('user:delete')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deleted"));
    }
}