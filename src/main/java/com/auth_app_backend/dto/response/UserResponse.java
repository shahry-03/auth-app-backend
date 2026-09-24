package com.auth_app_backend.dto.response;

import com.auth_app_backend.entity.Provider;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String name,
    String image,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt,
    Provider provider,
    Set<RoleResponse> roles
) {}