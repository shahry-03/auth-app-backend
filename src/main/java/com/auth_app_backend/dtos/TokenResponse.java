package com.auth_app_backend.dtos;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserDto userDto) {

    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn, UserDto userDto) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn, userDto);
    }

}
