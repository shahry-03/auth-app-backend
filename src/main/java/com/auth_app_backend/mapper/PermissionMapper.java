package com.auth_app_backend.mapper;

import com.auth_app_backend.dto.response.PermissionResponse;
import com.auth_app_backend.entity.Permission;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manual mapper for Permission entity ↔ DTOs.
 * Static methods — no Spring bean needed, no dependencies.
 */
public final class PermissionMapper {

    private PermissionMapper() {
        // utility class — no instances
    }

    /**
     * Entity → Response DTO
     */
    public static PermissionResponse toResponse(Permission permission) {
        if (permission == null) return null;
        return new PermissionResponse(
            permission.getId(),
            permission.getName(),
            permission.getDescription()
        );
    }

    /**
     * Set of entities → Set of response DTOs (null-safe)
     */
    public static Set<PermissionResponse> toResponseSet(Set<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptySet();
        }
        return permissions.stream()
            .map(PermissionMapper::toResponse)
            .collect(Collectors.toSet());
    }
}