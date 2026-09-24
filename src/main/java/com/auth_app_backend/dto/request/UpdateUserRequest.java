package com.auth_app_backend.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Partial update request — all fields optional.
 * Only non-null fields will be updated.
 */
public record UpdateUserRequest(

    @Size(max = 500, message = "Name must not exceed 500 characters")
    String name,

    @Size(max = 1000, message = "Image URL must not exceed 1000 characters")
    String image,

    Boolean enabled
) {}