package com.auth_app_backend.controller;

import com.auth_app_backend.config.UniversalAuthProperties;
import com.auth_app_backend.controllers.AdminPermissionController;
import com.auth_app_backend.dto.response.PermissionResponse;
import com.auth_app_backend.exception.ResourceNotFoundException;
import com.auth_app_backend.security.CookieService;
import com.auth_app_backend.security.JwtAuthenticationFilter;
import com.auth_app_backend.security.JwtService;
import com.auth_app_backend.services.PermissionService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminPermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "universal.auth.jwt.secret=test-secret-key-that-is-at-least-64-bytes-long-for-testing-purpose-only-x",
    "universal.auth.jwt.expiration=3600000",
    "universal.auth.jwt.refresh-token-expiration=604800000",
    "universal.auth.oauth2.enabled=false"
})
@DisplayName("AdminPermissionController Tests")
class AdminPermissionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private PermissionService permissionService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private CookieService cookieService;
    @MockBean private JwtService jwtService;
    @MockBean private UniversalAuthProperties properties;

    private UUID permissionId;
    private PermissionResponse permissionResponse;

    @BeforeEach
    void setUp() {
        permissionId = UUID.randomUUID();
        permissionResponse = new PermissionResponse(
            permissionId, "user:read", "Read user details"
        );
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/v1/admin/permissions
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/admin/permissions")
    class ListPermissions {

        @Test
        @DisplayName("Should return list of permissions")
        void shouldReturnList() throws Exception {
            when(permissionService.getAllPermissions())
                .thenReturn(List.of(permissionResponse));

            mockMvc.perform(get("/api/v1/admin/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("user:read"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/v1/admin/permissions/{id}
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/admin/permissions/{id}")
    class GetPermission {

        @Test
        @DisplayName("Should return permission by ID")
        void shouldReturn() throws Exception {
            when(permissionService.getPermissionById(permissionId))
                .thenReturn(permissionResponse);

            mockMvc.perform(get("/api/v1/admin/permissions/{id}", permissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(permissionId.toString()))
                .andExpect(jsonPath("$.data.name").value("user:read"));
        }

        @Test
        @DisplayName("Should return 404 when not found")
        void shouldReturn404() throws Exception {
            when(permissionService.getPermissionById(permissionId))
                .thenThrow(new ResourceNotFoundException("Permission not found"));

            mockMvc.perform(get("/api/v1/admin/permissions/{id}", permissionId))
                .andExpect(status().isNotFound());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  POST /api/v1/admin/permissions
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/admin/permissions")
    class CreatePermission {

        @Test
        @DisplayName("Should create permission")
        void shouldCreate() throws Exception {
            when(permissionService.createPermission(any(), any()))
                .thenReturn(permissionResponse);

            String body = """
                {
                  "name": "user:read",
                  "description": "Read user details"
                }
                """;

            mockMvc.perform(post("/api/v1/admin/permissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Permission created"))
                .andExpect(jsonPath("$.data.name").value("user:read"));

            verify(permissionService).createPermission(eq("user:read"), any());
        }

        @Test
        @DisplayName("Should return 400 when permission exists")
        void shouldReturn400OnDuplicate() throws Exception {
            when(permissionService.createPermission(any(), any()))
                .thenThrow(new IllegalArgumentException("Permission already exists: user:read"));

            String body = """
                {"name": "user:read", "description": "x"}
                """;

            mockMvc.perform(post("/api/v1/admin/permissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Permission already exists: user:read"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  PUT /api/v1/admin/permissions/{id}
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/v1/admin/permissions/{id}")
    class UpdatePermission {

        @Test
        @DisplayName("Should update permission")
        void shouldUpdate() throws Exception {
            when(permissionService.updatePermission(eq(permissionId), any()))
                .thenReturn(permissionResponse);

            String body = """
                {"description": "Updated description"}
                """;

            mockMvc.perform(put("/api/v1/admin/permissions/{id}", permissionId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Permission updated"));

            verify(permissionService).updatePermission(eq(permissionId), any());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  DELETE /api/v1/admin/permissions/{id}
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/v1/admin/permissions/{id}")
    class DeletePermission {

        @Test
        @DisplayName("Should delete permission")
        void shouldDelete() throws Exception {
            doNothing().when(permissionService).deletePermission(permissionId);

            mockMvc.perform(delete("/api/v1/admin/permissions/{id}", permissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Permission deleted"));

            verify(permissionService).deletePermission(permissionId);
        }

        @Test
        @DisplayName("Should return 404 when not found")
        void shouldReturn404() throws Exception {
            doThrow(new ResourceNotFoundException("Permission not found"))
                .when(permissionService).deletePermission(permissionId);

            mockMvc.perform(delete("/api/v1/admin/permissions/{id}", permissionId))
                .andExpect(status().isNotFound());
        }
    }
}