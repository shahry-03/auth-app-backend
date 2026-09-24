package com.auth_app_backend.config;

public class AppConstants {

    public static final String[] AUTH_PUBLIC_URLS = {
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/api/v1/auth/logout",
        "/api/v1/auth/verify-email",           
        "/api/v1/auth/resend-verification",    
        "/api/v1/auth/forgot-password",        
        "/api/v1/auth/reset-password",         
        
        // Dev-only test endpoints
        "/dev/email/**",

        // API docs
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**"

    };

    /**
     * URLs that require authentication but no specific role/permission.
     * @PreAuthorize handles the actual authorization at method level.
     */
    public static final String[] AUTHENTICATED_URLS = {
            "/api/v1/users/me",
            "/api/v1/users/me/**"
    };

    /**
     * Admin-only URL prefix — used for reference/documentation.
     * Actual access control is enforced via @PreAuthorize("hasRole('ADMIN')").
     */
    public static final String ADMIN_URL_PREFIX = "/api/v1/admin";
}