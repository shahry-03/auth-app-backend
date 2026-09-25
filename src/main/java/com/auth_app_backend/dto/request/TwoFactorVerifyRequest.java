package com.auth_app_backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorVerifyRequest(
    @NotBlank(message = "Temporary token is required")
    String tempToken,

    @NotBlank(message = "Code is required")
    String code               // 6-digit TOTP or backup code (XXXX-XXXX-XXXX)
) {}