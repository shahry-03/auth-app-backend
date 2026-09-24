package com.auth_app_backend.dto.internal;

import com.auth_app_backend.entity.Provider;

import java.util.Set;
import java.util.UUID;

/**
 * INTERNAL USE ONLY — service-to-service communication.
 *
 * WARNING: Contains hashed password. NEVER expose this DTO through a
 * public REST endpoint.
 *
 * @param authorities pre-built authority strings — BOTH roles and permissions
 *                    Example: ["ROLE_ADMIN", "ROLE_USER", "user:read", "user:write"]
 *                    Auth service directly uses these to build Authentication object.
 */
public record InternalUserResponse(
    UUID id,
    String email,
    String name,
    String password,
    boolean enabled,
    Provider provider,
    String providerId,
    Set<String> authorities      // ← flat set, ready-to-use
) {}