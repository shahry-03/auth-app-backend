package com.auth_app_backend.controller;

import com.auth_app_backend.config.UniversalAuthProperties;
import com.auth_app_backend.controllers.AdminRoleController;
import com.auth_app_backend.dto.response.RoleResponse;
import com.auth_app_backend.dto.response.UserResponse;
import com.auth_app_backend.exception.ResourceNotFoundException;
import com.auth_app_backend.security.CookieService;
import com.auth_app_backend.security.JwtAuthenticationFilter;
import com.auth_app_backend.security.JwtService;
import com.auth_app_backend.services.RoleService;
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

@WebMvcTest(AdminRoleController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "universal.auth.jwt.secret=test-secret-key-that-is-at-least-64-bytes-long-for-testing-purpose-only-x",
    "universal.auth.jwt.expiration=3600000",
    "universal.auth.jwt.refresh-token-expiration=604800000",
    "universal.auth.oauth2.enabled=false"
})
@DisplayName("AdminRoleController Tests")
class AdminRoleControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private RoleService roleService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private CookieService cookieService;
    @MockBean private JwtService jwtService;
    @MockBean private UniversalAuthProperties properties;

    private UUID roleId;
    private UUID userId;
    private RoleResponse roleResponse;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        roleId = UUID.randomUUID();
        userId = UUID.randomUUID();

        roleResponse = new RoleResponse(
            roleId, "MODERATOR", "Moderator role", Set.of()
        );

        userResponse = new UserResponse(
            userId, "user@test.com", "Test User", null,
            true, Instant.now(), Instant.now(), null, Set.of(roleResponse)
        );
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/v1/admin/roles
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/admin/roles")
    class ListRoles {

        @Test
        @DisplayName("Should return list of roles")
        void shouldReturnList() throws Exception {
            when(roleService.getAllRoles()).thenReturn(List.of(roleResponse));

            mockMvc.perform(get("/api/v1/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].roleName").value("MODERATOR"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/v1/admin/roles/{roleId}
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/admin/roles/{roleId}")
    class GetRole {

        @Test
        @DisplayName("Should return role by ID")
        void shouldReturn() throws Exception {
            when(roleService.getRoleById(roleId)).thenReturn(roleResponse);

            mockMvc.perform(get("/api/v1/admin/roles/{id}", roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(roleId.toString()))
                .andExpect(jsonPath("$.data.roleName").value("MODERATOR"));
        }

        @Test
        @DisplayName("Should return 404 when not found")
        void shouldReturn404() throws Exception {
            when(roleService.getRoleById(roleId))
                .thenThrow(new ResourceNotFoundException("Role not found"));

            mockMvc.perform(get("/api/v1/admin/roles/{id}", roleId))
                .andExpect(status().isNotFound());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  POST /api/v1/admin/roles
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/admin/roles")
    class CreateRole {

        @Test
        @DisplayName("Should create role with permissions")
        void shouldCreate() throws Exception {
            when(roleService.createRole(any(), any(), any())).thenReturn(roleResponse);

            String body = """
                {
                  "roleName": "MODERATOR",
                  "description": "Moderator role",
                  "permissions": ["user:read", "profile:read"]
                }
                """;

            mockMvc.perform(post("/api/v1/admin/roles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Role created"))
                .andExpect(jsonPath("$.data.roleName").value("MODERATOR"));

            verify(roleService).createRole(eq("MODERATOR"), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when role exists")
        void shouldReturn400OnDuplicate() throws Exception {
            when(roleService.createRole(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Role already exists: MODERATOR"));

            String body = """
                {"roleName": "MODERATOR", "description": "x", "permissions": []}
                """;

            mockMvc.perform(post("/api/v1/admin/roles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Role already exists: MODERATOR"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  PUT /api/v1/admin/roles/{roleId}
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/v1/admin/roles/{roleId}")
    class UpdateRole {

        @Test
        @DisplayName("Should update role")
        void shouldUpdate() throws Exception {
            when(roleService.updateRole(eq(roleId), any(), any())).thenReturn(roleResponse);

            String body = """
                {
                  "description": "Updated",
                  "permissions": ["user:read", "user:write"]
                }
                """;

            mockMvc.perform(put("/api/v1/admin/roles/{id}", roleId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role updated"));

            verify(roleService).updateRole(eq(roleId), any(), any());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  DELETE /api/v1/admin/roles/{roleId}
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/v1/admin/roles/{roleId}")
    class DeleteRole {

        @Test
        @DisplayName("Should delete custom role")
        void shouldDelete() throws Exception {
            doNothing().when(roleService).deleteRole(roleId);

            mockMvc.perform(delete("/api/v1/admin/roles/{id}", roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role deleted"));

            verify(roleService).deleteRole(roleId);
        }

        @Test
        @DisplayName("Should return 400 for built-in role")
        void shouldRejectBuiltIn() throws Exception {
            doThrow(new IllegalArgumentException("Cannot delete built-in role: ADMIN"))
                .when(roleService).deleteRole(roleId);

            mockMvc.perform(delete("/api/v1/admin/roles/{id}", roleId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot delete built-in role: ADMIN"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  Role Assignment
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Role Assignment")
    class RoleAssignment {

        @Test
        @DisplayName("assignRole — should assign role to user")
        void shouldAssign() throws Exception {
            when(roleService.assignRoleToUser(userId, roleId)).thenReturn(userResponse);

            mockMvc.perform(post("/api/v1/admin/roles/users/{uid}/assign/{rid}", userId, roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role assigned"));

            verify(roleService).assignRoleToUser(userId, roleId);
        }

        @Test
        @DisplayName("removeRole — should remove role from user")
        void shouldRemove() throws Exception {
            when(roleService.removeRoleFromUser(userId, roleId)).thenReturn(userResponse);

            mockMvc.perform(delete("/api/v1/admin/roles/users/{uid}/remove/{rid}", userId, roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role removed"));

            verify(roleService).removeRoleFromUser(userId, roleId);
        }

        @Test
        @DisplayName("getUserRoles — should return user's roles")
        void shouldGetUserRoles() throws Exception {
            when(roleService.getUserRoles(userId)).thenReturn(Set.of(roleResponse));

            mockMvc.perform(get("/api/v1/admin/roles/users/{uid}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].roleName").value("MODERATOR"));
        }
    }
}