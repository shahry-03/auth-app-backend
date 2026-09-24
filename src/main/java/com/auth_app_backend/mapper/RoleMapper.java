package com.auth_app_backend.mapper;

import com.auth_app_backend.dto.response.RoleResponse;
import com.auth_app_backend.entity.Role;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manual mapper for Role entity ↔ DTOs.
 */
public final class RoleMapper {

    private RoleMapper() {
        // utility class
    }

    /**
     * Entity → Response DTO
     * Includes permissions (nested).
     */
    public static RoleResponse toResponse(Role role) {
        if (role == null) return null;
        return new RoleResponse(
            role.getId(),
            role.getRoleName(),
            role.getDescription(),
            PermissionMapper.toResponseSet(role.getPermissions())
        );
    }

    /**
     * Entity → Response DTO, WITHOUT permissions.
     * Use when you need just role name (e.g., in UserResponse listing).
     */
    public static RoleResponse toSummary(Role role) {
        if (role == null) return null;
        return new RoleResponse(
            role.getId(),
            role.getRoleName(),
            role.getDescription(),
            Collections.emptySet()
        );
    }

    /**
     * Set of entities → Set of response DTOs (null-safe)
     */
    public static Set<RoleResponse> toResponseSet(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptySet();
        }
        return roles.stream()
            .map(RoleMapper::toResponse)
            .collect(Collectors.toSet());
    }

    /**
     * Set of entities → Set of summary DTOs (without permissions).
     */
    public static Set<RoleResponse> toSummarySet(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptySet();
        }
        return roles.stream()
            .map(RoleMapper::toSummary)
            .collect(Collectors.toSet());
    }
}