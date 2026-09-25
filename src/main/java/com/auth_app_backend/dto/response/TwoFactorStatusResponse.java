package com.auth_app_backend.dto.response;

public record TwoFactorStatusResponse(
    boolean enabled,
    long backupCodesRemaining
) {}