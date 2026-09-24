package com.auth_app_backend.controller;

import com.auth_app_backend.config.UniversalAuthProperties;
import com.auth_app_backend.controllers.AdminUserController;
import com.auth_app_backend.dto.request.UpdateUserRequest;
import com.auth_app_backend.dto.response.UserResponse;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.exception.ResourceNotFoundException;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "universal.auth.jwt.secret=test-secret-key-that-is-at-least-64-bytes-long-for-testing-purpose-only-x",
    "universal.auth.jwt.expiration=3600000",
    "universal.auth.jwt.refresh-token-expiration=604800000",
    "universal.auth.oauth2.enabled=false"
})
@DisplayName("AdminUserController Tests")
class AdminUserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private CookieService cookieService;
    @MockBean private JwtService jwtService;
    @MockBean private UniversalAuthProperties properties;

    private UUID userId;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userResponse = new UserResponse(
            userId, "user@test.com", "Test User", null,
            true, Instant.now(), Instant.now(), null, Set.of()
        );
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/v1/admin/users
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/admin/users")
    class ListUsers {

        @Test
        @DisplayName("Should return list of users")
        void shouldReturnList() throws Exception {
            when(userService.getAllUsers()).thenReturn(List.of(userResponse));

            mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].email").value("user@test.com"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/v1/admin/users/{userId}
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/admin/users/{userId}")
    class GetUser {

        @Test
        @DisplayName("Should return user by ID")
        void shouldReturnUser() throws Exception {
            when(userService.getUserById(userId)).thenReturn(userResponse);

            mockMvc.perform(get("/api/v1/admin/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").value("user@test.com"));
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404() throws Exception {
            when(userService.getUserById(userId))
                .thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(get("/api/v1/admin/users/{id}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  PUT /api/v1/admin/users/{userId}
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/v1/admin/users/{userId}")
    class UpdateUser {

        @Test
        @DisplayName("Should update user")
        void shouldUpdate() throws Exception {
            when(userService.updateUser(eq(userId), any(UpdateUserRequest.class)))
                .thenReturn(userResponse);

            String body = """
                {"name": "Updated", "enabled": true}
                """;

            mockMvc.perform(put("/api/v1/admin/users/{id}", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User updated"));

            verify(userService).updateUser(eq(userId), any());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  PATCH /api/v1/admin/users/{userId}/status
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PATCH /api/v1/admin/users/{userId}/status")
    class EnableDisable {

        @Test
        @DisplayName("Should enable user")
        void shouldEnable() throws Exception {
            doNothing().when(userService).enableUser(userId, true);

            mockMvc.perform(patch("/api/v1/admin/users/{id}/status", userId)
                    .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User enabled"));

            verify(userService).enableUser(userId, true);
        }

        @Test
        @DisplayName("Should disable user")
        void shouldDisable() throws Exception {
            doNothing().when(userService).enableUser(userId, false);

            mockMvc.perform(patch("/api/v1/admin/users/{id}/status", userId)
                    .param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User disabled"));

            verify(userService).enableUser(userId, false);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  DELETE /api/v1/admin/users/{userId}
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/v1/admin/users/{userId}")
    class DeleteUser {

        @Test
        @DisplayName("Should delete user")
        void shouldDelete() throws Exception {
            doNothing().when(userService).deleteUser(userId);

            mockMvc.perform(delete("/api/v1/admin/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted"));

            verify(userService).deleteUser(userId);
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404() throws Exception {
            doThrow(new ResourceNotFoundException("User not found"))
                .when(userService).deleteUser(userId);

            mockMvc.perform(delete("/api/v1/admin/users/{id}", userId))
                .andExpect(status().isNotFound());
        }
    }
}