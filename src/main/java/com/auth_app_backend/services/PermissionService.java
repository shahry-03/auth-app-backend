package com.auth_app_backend.services;

import com.auth_app_backend.dto.response.PermissionResponse;

import java.util.List;
import java.util.UUID;

public interface PermissionService {

    PermissionResponse getPermissionById(UUID permissionId);

    List<PermissionResponse> getAllPermissions();

    PermissionResponse createPermission(String name, String description);

    PermissionResponse updatePermission(UUID permissionId, String description);

    void deletePermission(UUID permissionId);
}