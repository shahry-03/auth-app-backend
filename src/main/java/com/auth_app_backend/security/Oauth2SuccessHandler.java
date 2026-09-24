package com.auth_app_backend.security;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.auth_app_backend.config.UniversalAuthProperties;
import com.auth_app_backend.entity.Provider;
import com.auth_app_backend.entity.RefreshToken;
import com.auth_app_backend.entity.Role;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.exception.ResourceNotFoundException;
import com.auth_app_backend.repositories.RoleRepository;
import com.auth_app_backend.repositories.UserRepository;
import com.auth_app_backend.services.RefreshTokenService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class Oauth2SuccessHandler implements AuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final CookieService cookieService;
    private final String frontendSuccessRedirect;

    public Oauth2SuccessHandler(UserRepository userRepository,
                                RoleRepository roleRepository,
                                JwtService jwtService,
                                RefreshTokenService refreshTokenService,
                                CookieService cookieService,
                                UniversalAuthProperties properties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.cookieService = cookieService;
        this.frontendSuccessRedirect = properties.getFrontend().getSuccessRedirect();
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        String registrationId = "unknown";
        if (authentication instanceof OAuth2AuthenticationToken token) {
            registrationId = token.getAuthorizedClientRegistrationId();
        }

        logger.info("OAuth2 login success via provider: {}", registrationId);

        User user = switch (registrationId) {
            case "google" -> handleGoogle(oauth2User);
            case "github" -> handleGithub(oauth2User);
            default -> throw new IllegalArgumentException("Unknown provider: " + registrationId);
        };

        // Create refresh token via service (single source of truth)
        RefreshToken refreshToken = refreshTokenService.createForUser(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken(user, refreshToken.getJti());

        // Cookie maxAge is in SECONDS — convert from millis
        int maxAgeSeconds = (int) (jwtService.getRefreshExpirationInMillis() / 1000);
        cookieService.attachRefreshCookie(response, refreshTokenValue, maxAgeSeconds);

        response.sendRedirect(frontendSuccessRedirect);
    }

    // ─────────────────────────────────────────────
    //  PROVIDER HANDLERS
    // ─────────────────────────────────────────────

    private User handleGoogle(OAuth2User oauth2User) {
        var attrs = oauth2User.getAttributes();
        String googleId = String.valueOf(attrs.getOrDefault("sub", ""));
        String email    = String.valueOf(attrs.getOrDefault("email", ""));
        String name     = String.valueOf(attrs.getOrDefault("name", ""));
        String picture  = String.valueOf(attrs.getOrDefault("picture", ""));

        return findOrCreateUser(email, name, picture, Provider.GOOGLE, googleId);
    }

    private User handleGithub(OAuth2User oauth2User) {
        var attrs = oauth2User.getAttributes();
        String githubId = String.valueOf(attrs.getOrDefault("id", ""));
        String login    = String.valueOf(attrs.getOrDefault("login", ""));
        String email    = String.valueOf(attrs.getOrDefault("email", ""));
        String picture  = String.valueOf(attrs.getOrDefault("avatar_url", ""));

        // GitHub may not expose email — use provider id based placeholder (stable)
        if (email == null || email.isBlank() || "null".equals(email)) {
            email = githubId + "@github.placeholder";
        }

        String name = (login == null || login.isBlank()) ? "github-user" : login;
        return findOrCreateUser(email, name, picture, Provider.GITHUB, githubId);
    }

    private User findOrCreateUser(String email, String name, String image,
                                  Provider provider, String providerId) {

        return userRepository.findByEmail(email).orElseGet(() -> {

            Role defaultRole = roleRepository.findByRoleName("USER")
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Default USER role not found. Check RbacDataInitializer."));

            User newUser = User.builder()
                .email(email)
                .name(name)
                .image(image)
                .provider(provider)
                .providerId(providerId)
                .enabled(true)
                .roles(new HashSet<>(Set.of(defaultRole)))   // ✅ RBAC default role
                .build();

            return userRepository.save(newUser);
        });
    }
}