package com.auth_app_backend.dto.response;

import java.util.List;

/**
 * Response from /2fa/enable — contains backup codes.
 * ⚠️ Backup codes are shown ONLY ONCE. User must save them.
 */
public record TwoFactorEnableResponse(
    boolean enabled,
    List<String> backupCodes
) {}