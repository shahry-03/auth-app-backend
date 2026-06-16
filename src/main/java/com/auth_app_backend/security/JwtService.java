package com.auth_app_backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth_app_backend.entity.Role;
import com.auth_app_backend.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;

@Service
@Getter
@Setter
public class JwtService {
    private final SecretKey secretKey;
    private final long jwtExpirationInMillis;
    private final long refreshExpirationInMillis;
    private final String jwtIssuer;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration}") long jwtExpirationInMillis,
            @Value("${security.jwt.refresh-token-expiration}") long refreshExpirationInMillis,
            @Value("${security.jwt.issuer}") String jwtIssuer) {

        if (secret == null || secret.length() < 64) {
            throw new IllegalArgumentException("Secret key must be at least 64 bytes long");
        }

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationInMillis = jwtExpirationInMillis;
        this.refreshExpirationInMillis = refreshExpirationInMillis;
        this.jwtIssuer = jwtIssuer;
    }

    // Generate JWT token
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles() == null ? List.of()
                : user.getRoles().stream()
                        .map(Role::getRoleName)
                        .toList();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .issuer(jwtIssuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(jwtExpirationInMillis)))
                .claims(Map.of(
                        "email", user.getEmail(),
                        "roles", roles,
                        "type", "access"))
                .signWith(secretKey)
                .compact();
    }

    // generate refresh token
    public String generateRefreshToken(User user, String jti) {
        Instant now = Instant.now();
        // Implementation to generate JWT refresh token
        return Jwts.builder()
                .id(jti)
                .subject(user.getId().toString())
                .issuer(jwtIssuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(refreshExpirationInMillis)))
                .claim("type", "refresh")
                .signWith(secretKey)
                .compact();
    }

    // Parse the token
    public Jws<Claims> parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
        } catch (Exception e) {
            // Handle token parsing exceptions (e.g., expired, invalid signature)
            throw new RuntimeException("Invalid or expired token", e);
        }
    }

    // Check if the token is an access token
    public boolean isAccessToken(String token) {
        try {
            Claims claims = parseToken(token).getPayload();
            return "access".equals(claims.get("type"));
        } catch (Exception e) {
            // if token is expired, malformed, or has invalid signature, consider it not an
            // access token and return false
            return false;
        }
    }

    // Check if the token is a refresh token
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseToken(token).getPayload();
            return "refresh".equals(claims.get("type"));
        } catch (Exception e) {
            // if token is expired, malformed, or has invalid signature, consider it not a
            // refresh token and return false
            return false;
        }
    }

    // Extract user ID from token
    public UUID getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token).getPayload();
            return UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException e) {
            // Handle invalid UUID format
            throw new RuntimeException("Invalid user ID format", e);
        } catch (Exception e) {
            // Handle token parsing exceptions (e.g., expired, invalid signature)
            throw new RuntimeException("Invalid or expired token", e);
        }
    }

    // Extract JTI from token
    public String getJtiFromToken(String token) {
        try {
            Claims claims = parseToken(token).getPayload();
            return claims.getId();
        } catch (Exception e) {
            // Handle token parsing exceptions (e.g., expired, invalid signature)
            throw new RuntimeException("Invalid or expired token", e);
        }
    }

    // Extract roles from token
    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = parseToken(token).getPayload();
            return claims.get("roles", List.class);
        } catch (Exception e) {
            // Handle token parsing exceptions (e.g., expired, invalid signature)
            throw new RuntimeException("Invalid or expired token", e);
        }
    }

    // Extract email from token
    public String getEmailFromToken(String token) {
        try {
            Claims claims = parseToken(token).getPayload();
            return claims.get("email", String.class);
        } catch (Exception e) {
            // Handle token parsing exceptions (e.g., expired, invalid signature)
            throw new RuntimeException("Invalid or expired token", e);
        }
    }

    // Validate the token (e.g., check expiration, signature)
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            // Handle token validation exceptions (e.g., expired, invalid signature)
            return false;
        }
    }

}
