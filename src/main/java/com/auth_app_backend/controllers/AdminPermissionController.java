package com.auth_app_backend.controllers;

import com.auth_app_backend.dto.response.ApiResponse;
import com.auth_app_backend.dto.response.PermissionResponse;
import com.auth_app_backend.services.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('role:manage')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
        return ResponseEntity.ok(ApiResponse.success(permissionService.getAllPermissions()));
    }

    @GetMapping("/{permissionId}")
    @PreAuthorize("hasAuthority('role:manage')")
    public ResponseEntity<ApiResponse<PermissionResponse>> getPermission(
            @PathVariable UUID permissionId) {
        return ResponseEntity.ok(ApiResponse.success(
            permissionService.getPermissionById(permissionId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:manage')")
    public ResponseEntity<ApiResponse<PermissionResponse>> createPermission(
            @RequestBody CreatePermissionRequest request) {
        PermissionResponse p = permissionService.createPermission(
            request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Permission created", p));
    }

    @PutMapping("/{permissionId}")
    @PreAuthorize("hasAuthority('role:manage')")
    public ResponseEntity<ApiResponse<PermissionResponse>> updatePermission(
            @PathVariable UUID permissionId,
            @RequestBody UpdatePermissionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Permission updated",
            permissionService.updatePermission(permissionId, request.description())));
    }

    @DeleteMapping("/{permissionId}")
    @PreAuthorize("hasAuthority('role:manage')")
    public ResponseEntity<ApiResponse<Void>> deletePermission(@PathVariable UUID permissionId) {
        permissionService.deletePermission(permissionId);
        return ResponseEntity.ok(ApiResponse.success("Permission deleted"));
    }

    public record CreatePermissionRequest(String name, String description) {}
    public record UpdatePermissionRequest(String description) {}
}