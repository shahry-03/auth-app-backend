package com.auth_app_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Generic registration request.
 * Role assignment registration pe NAHI hota — default role assign hota hai
 * (e.g., "USER"). Admin baad me roles assign karta hai.
 */
public record RegisterRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 300, message = "Email must not exceed 300 characters")
    String email,

    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
        message = "Password must be 8+ characters with at least one letter, one number, and one special character"
    )
    String password,

    @NotBlank(message = "Name is required")
    @Size(max = 500, message = "Name must not exceed 500 characters")
    String name
) {}