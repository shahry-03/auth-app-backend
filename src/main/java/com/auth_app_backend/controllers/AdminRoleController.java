package com.auth_app_backend.controllers;

import com.auth_app_backend.dto.response.ApiResponse;
import com.auth_app_backend.dto.response.RoleResponse;
import com.auth_app_backend.dto.response.UserResponse;
import com.auth_app_backend.services.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('role:manage')")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.success(roleService.getAllRoles()));
    }

    @GetMapping("/{roleId}")
    @PreAuthorize("hasAuthority('role:manage')")
    public ResponseEntity<ApiResponse<RoleResponse>> getRole(@PathVariable UUID roleId) {
        return ResponseEntity.ok(ApiResponse.success(roleService.getRoleById(roleId)));
    }

    /**
     * Create a new role with permissions.
     * Request body:
     * {
     *   "roleName": "SHOP_OWNER",
     *   "description": "Shop owner role",
     *   "permissions": ["user:read", "profile:write"]
     * }
     */
    @PostMapping
    @PreAuthorize("hasAuthority('role:manage')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @RequestBody CreateRoleRequest request) {
        RoleResponse role = roleService.createRole(
            request.roleName(), request.description(), request.permissions());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Role created", role));
    }

    @PutMapping("/{roleId}")
    @PreAuthorize("hasAuthority('role:manage')")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable UUID roleId,
            @RequestBody UpdateRoleRequest request) {
        RoleResponse role = roleService.updateRole(
            roleId, request.description(), request.permissions());
        return ResponseEntity.ok(ApiResponse.success("Role updated", role));
    }

    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasAuthority('role:manage')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable UUID roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.ok(ApiResponse.success("Role deleted"));
    }

    // ---- Role Assignment ----

    /**
     * Assign a role to a user.
     */
    @PostMapping("/users/{userId}/assign/{roleId}")
    @PreAuthorize("hasAuthority('role:manage')")
    public ResponseEntity<ApiResponse<UserResponse>> assignRole(
            @PathVariable UUID userId,
            @PathVariable UUID roleId) {
        UserResponse user = roleService.assignRoleToUser(userId, roleId);
        return ResponseEntity.ok(ApiResponse.success("Role assigned", user));
    }

    /**
     * Remove a role from a user.
     */
    @DeleteMapping("/users/{userId}/remove/{roleId}")
    @PreAuthorize("hasAuthority('role:manage')")
    public ResponseEntity<ApiResponse<UserResponse>> removeRole(
            @PathVariable UUID userId,
            @PathVariable UUID roleId) {
        UserResponse user = roleService.removeRoleFromUser(userId, roleId);
        return ResponseEntity.ok(ApiResponse.success("Role removed", user));
    }

    /**
     * Get all roles of a user.
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('role:manage')")
    public ResponseEntity<ApiResponse<Set<RoleResponse>>> getUserRoles(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(roleService.getUserRoles(userId)));
    }

    // ---- Nested Request Records ----

    public record CreateRoleRequest(
        String roleName,
        String description,
        Set<String> permissions
    ) {}

    public record UpdateRoleRequest(
        String description,
        Set<String> permissions
    ) {}
}