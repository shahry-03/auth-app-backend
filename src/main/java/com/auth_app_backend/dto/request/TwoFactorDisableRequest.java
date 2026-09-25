package com.auth_app_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TwoFactorDisableRequest(
    @NotBlank(message = "Password is required")
    String password,

    @NotBlank(message = "Code is required")
    @Pattern(regexp = "^\\d{6}$", message = "Code must be 6 digits")
    String code
) {}