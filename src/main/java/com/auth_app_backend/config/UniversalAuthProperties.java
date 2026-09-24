package com.auth_app_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "universal.auth")
@Getter
@Setter
public class UniversalAuthProperties {
    
    private Jwt jwt = new Jwt();
    private Oauth2 oauth2 = new Oauth2();
    private Cookie cookie = new Cookie();
    private Frontend frontend = new Frontend();

    @Getter
    @Setter
    public static class Cookie {
        private String refreshTokenName = "refresh_token";
        private boolean secure = false; // set to true in production
        private boolean httpOnly = true;
        private String domain = "";
        private String sameSite = "Lax";
    }

    @Getter
    @Setter
    public static class Frontend {
        private String successRedirect = "http://localhost:3000/dashboard";
    }

    @Getter
    @Setter
    public static class Oauth2 {
        /**
         * Enable or disable OAuth2 login.
         */
        private boolean enabled = false;
    }

    @Getter
    @Setter
    public static class Jwt {
        /**
         * Secret key used for signing JWTs. Must be at least 64 bytes long.
         */
        private String secret = "this-is-a-default-secret-key-that-should-be-changed-in-production-environments-at-least-64-bytes";
        
        /**
         * Access token expiration time in milliseconds.
         * Default: 1 hour (3600000 ms)
         */
        private long expiration = 3600000;
        
        /**
         * Refresh token expiration time in milliseconds.
         * Default: 7 days (604800000 ms)
         */
        private long refreshTokenExpiration = 604800000;
        
        /**
         * The issuer to attach to the JWT token.
         */
        private String issuer = "universal-auth";
    }
}
