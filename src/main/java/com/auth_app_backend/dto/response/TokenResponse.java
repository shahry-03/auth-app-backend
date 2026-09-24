package com.auth_app_backend.dto.response;

/**
 * @param expiresIn access token expiry in seconds
 */
public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    UserResponse user
) {
    public static TokenResponse of(String accessToken, String refreshToken,
                                   long expiresIn, UserResponse user) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}