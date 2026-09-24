package com.auth_app_backend.controllers;

import com.auth_app_backend.entity.User;
import com.auth_app_backend.mapper.UserMapper;
import com.auth_app_backend.dto.request.ChangePasswordRequest;
import com.auth_app_backend.dto.request.UpdateUserRequest;
import com.auth_app_backend.dto.response.ApiResponse;
import com.auth_app_backend.dto.response.UserResponse;
import com.auth_app_backend.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get current logged-in user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal User user) {                                 // ← User directly
        return ResponseEntity.ok(ApiResponse.success(UserMapper.toResponse(user)));   // ← No DB call
    }

    /**
     * Update own profile.
     * Requires 'profile:write' permission.
     */
    @PutMapping("/me")
    @PreAuthorize("hasAuthority('profile:write')")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse me = userService.getUserByEmail(userDetails.getUsername());
        UserResponse updated = userService.updateUser(me.id(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", updated));
    }

    /**
     * Change own password.
     * Requires 'profile:write' permission.
     */
    @PostMapping("/me/change-password")
    @PreAuthorize("hasAuthority('profile:write')")
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        UserResponse me = userService.getUserByEmail(userDetails.getUsername());
        userService.changePassword(me.id(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
}