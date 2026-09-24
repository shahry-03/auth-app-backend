package com.auth_app_backend.controller;

import com.auth_app_backend.config.UniversalAuthProperties;
import com.auth_app_backend.controllers.UserController;
import com.auth_app_backend.dto.response.UserResponse;
import com.auth_app_backend.entity.Role;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.security.CookieService;
import com.auth_app_backend.security.JwtAuthenticationFilter;
import com.auth_app_backend.security.JwtService;
import com.auth_app_backend.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "universal.auth.jwt.secret=test-secret-key-that-is-at-least-64-bytes-long-for-testing-purpose-only-x",
    "universal.auth.jwt.expiration=3600000",
    "universal.auth.jwt.refresh-token-expiration=604800000",
    "universal.auth.oauth2.enabled=false"
})
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private CookieService cookieService;
    @MockBean private JwtService jwtService;
    @MockBean private UniversalAuthProperties properties;

    private User testUser;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();

        Role userRole = Role.builder()
            .id(UUID.randomUUID())
            .roleName("USER")
            .description("Default user role")
            .permissions(new HashSet<>())
            .build();

        testUser = User.builder()
            .id(userId)
            .email("test@example.com")
            .name("Test User")
            .password("hashed")
            .enabled(true)
            .roles(new HashSet<>(Set.of(userRole)))
            .build();

        userResponse = new UserResponse(
            userId, "test@example.com", "Test User", null,
            true, Instant.now(), Instant.now(), null, Set.of()
        );
    }

    /**
     * Custom RequestPostProcessor that sets SecurityContextHolder directly.
     * This is required because addFilters=false bypasses the filter chain
     * that normally restores SecurityContext from session.
     */
    private RequestPostProcessor authenticateAs(User user) {
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        return request -> {
            SecurityContextHolder.getContext().setAuthentication(auth);
            return request;
        };
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/v1/users/me
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/users/me")
    class GetMyProfile {

        @Test
        @DisplayName("Should return own profile when authenticated")
        void shouldReturnProfile() throws Exception {
            mockMvc.perform(get("/api/v1/users/me")
                    .with(authenticateAs(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.name").value("Test User"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  PUT /api/v1/users/me
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/v1/users/me")
    class UpdateMyProfile {

        @Test
        @DisplayName("Should update profile with valid data")
        void shouldUpdateProfile() throws Exception {
            when(userService.updateUser(eq(testUser.getId()), any()))
                .thenReturn(userResponse);

            String body = """
                {
                  "name": "Updated Name",
                  "image": "https://example.com/avatar.png"
                }
                """;

            mockMvc.perform(put("/api/v1/users/me")
                    .with(authenticateAs(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));

            verify(userService).updateUser(eq(testUser.getId()), any());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  POST /api/v1/users/me/change-password
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/users/me/change-password")
    class ChangeMyPassword {

        @Test
        @DisplayName("Should change password with valid request")
        void shouldChangePassword() throws Exception {
            doNothing().when(userService).changePassword(eq(testUser.getId()), any());

            String body = """
                {
                  "currentPassword": "Old@1234",
                  "newPassword": "New@5678"
                }
                """;

            mockMvc.perform(post("/api/v1/users/me/change-password")
                    .with(authenticateAs(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

            verify(userService).changePassword(eq(testUser.getId()), any());
        }

        @Test
        @DisplayName("Should return 400 on weak new password")
        void shouldReturn400OnWeakPassword() throws Exception {
            String body = """
                {
                  "currentPassword": "Old@1234",
                  "newPassword": "123"
                }
                """;

            mockMvc.perform(post("/api/v1/users/me/change-password")
                    .with(authenticateAs(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());

            verify(userService, never()).changePassword(any(), any());
        }

        @Test
        @DisplayName("Should propagate wrong password error as 400")
        void shouldPropagateWrongPassword() throws Exception {
            doThrow(new IllegalArgumentException("Current password is incorrect"))
                .when(userService).changePassword(eq(testUser.getId()), any());

            String body = """
                {
                  "currentPassword": "Wrong@1234",
                  "newPassword": "New@5678"
                }
                """;

            mockMvc.perform(post("/api/v1/users/me/change-password")
                    .with(authenticateAs(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));
        }
    }
}