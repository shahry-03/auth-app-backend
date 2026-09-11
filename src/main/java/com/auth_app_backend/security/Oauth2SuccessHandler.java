package com.auth_app_backend.security;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.auth_app_backend.entity.Provider;
import com.auth_app_backend.entity.RefreshToken;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.repositories.RefreshTokenRepository;
import com.auth_app_backend.repositories.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.auth_app_backend.config.UniversalAuthProperties;

@Component
public class Oauth2SuccessHandler implements AuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final String frontendSuccessRedirect;
    private final CookieService cookieService;

    public Oauth2SuccessHandler(UserRepository userRepository, JwtService jwtService,
            RefreshTokenRepository refreshTokenRepository, UniversalAuthProperties properties, CookieService cookieService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.frontendSuccessRedirect = properties.getFrontend().getSuccessRedirect();
        this.cookieService = cookieService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        logger.info("Authentication successful for user: {}", authentication.getName());
        logger.info(authentication.toString());

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        // Identify the user
        String registrationId = "unknown";
        if (authentication instanceof OAuth2AuthenticationToken token) {
            registrationId = token.getAuthorizedClientRegistrationId();

        }
        logger.info("registrationId:" + registrationId);
        logger.info("user attributes: " + oauth2User.getAttributes().toString());

        User user;
        switch (registrationId) {
            case "google" -> {
                // Handle Google authentication
                String googleId = oauth2User.getAttributes().getOrDefault("sub", "").toString();
                String email = oauth2User.getAttributes().getOrDefault("email", "").toString();
                String name = oauth2User.getAttributes().getOrDefault("name", "").toString();
                String picture = oauth2User.getAttributes().getOrDefault("picture", "").toString();
                User newUser = User.builder()
                        .email(email)
                        .name(name)
                        .image(picture)
                        .provider(Provider.GOOGLE)
                        .providerId(googleId)
                        .build();

                user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(newUser));
            }
            case "github" -> {
                // Handle GitHub authentication
                String githubId = oauth2User.getAttributes().getOrDefault("id", "").toString();
                String name = oauth2User.getAttributes().getOrDefault("login", "").toString();
                String email = oauth2User.getAttributes().getOrDefault("email", "").toString();
                if (email == null || email.isEmpty()) {
                    // If email is not provided, you might want to fetch it from another endpoint or
                    // handle it accordingly
                    email = name + "@github.com"; // Placeholder email if GitHub doesn't provide one

                }
                String picture = oauth2User.getAttributes().getOrDefault("avatar_url", "").toString();
                User newUser = User.builder()
                        .email(email)
                        .name(name)
                        .image(picture)
                        .provider(Provider.GITHUB)
                        .providerId(githubId)
                        .build();
                user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(newUser));
            }
            default -> {
                // Handle unknown provider
                throw new IllegalArgumentException("Unknown provider: " + registrationId);

            }
        }

        // create refresh token
        String jti = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .revoked(false)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshExpirationInMillis()))
                .build();

        refreshTokenRepository.save(refreshToken);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenString = jwtService.generateRefreshToken(user, refreshToken.getJti());

        cookieService.attachRefreshCookie(response, refreshTokenString,
                (int) jwtService.getRefreshExpirationInMillis());

        // response.getWriter().write("Login Successful");

        response.sendRedirect(frontendSuccessRedirect);

    }

}
