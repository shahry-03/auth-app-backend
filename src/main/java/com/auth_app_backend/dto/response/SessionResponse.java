package com.auth_app_backend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
    UUID id,
    Instant createdAt,
    Instant expiresAt,
    Instant lastUsedAt,
    String ipAddress,
    String userAgent,
    boolean current
) {}