package com.auth_app_backend.service;

import com.auth_app_backend.dto.response.RoleResponse;
import com.auth_app_backend.dto.response.UserResponse;
import com.auth_app_backend.entity.Permission;
import com.auth_app_backend.entity.Role;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.exception.ResourceNotFoundException;
import com.auth_app_backend.repositories.PermissionRepository;
import com.auth_app_backend.repositories.RoleRepository;
import com.auth_app_backend.repositories.UserRepository;
import com.auth_app_backend.services.impl.RoleServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService Tests")
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    // ═══════════════════════════════════════════════════════════
    //  Fixtures
    // ═══════════════════════════════════════════════════════════

    private Role createRole(UUID id, String name) {
        return Role.builder()
            .id(id)
            .roleName(name)
            .description(name + " role")
            .permissions(new HashSet<>())
            .build();
    }

    private Permission createPermission(UUID id, String name) {
        return Permission.builder()
            .id(id)
            .name(name)
            .description(name + " desc")
            .build();
    }

    private User createUser(UUID id, String email, Set<Role> roles) {
        return User.builder()
            .id(id)
            .email(email)
            .name("Test")
            .password("hashed")
            .enabled(true)
            .roles(new HashSet<>(roles))
            .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  getRoleById / getRoleByName / getAllRoles
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Read Operations")
    class ReadOperations {

        @Test
        @DisplayName("getRoleById — should return role when found")
        void getById() {
            UUID id = UUID.randomUUID();
            Role role = createRole(id, "ADMIN");
            when(roleRepository.findById(id)).thenReturn(Optional.of(role));

            RoleResponse result = roleService.getRoleById(id);

            assertThat(result.id()).isEqualTo(id);
            assertThat(result.roleName()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("getRoleById — should throw when not found")
        void getByIdNotFound() {
            UUID id = UUID.randomUUID();
            when(roleRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.getRoleById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role not found");
        }

        @Test
        @DisplayName("getRoleByName — should return role")
        void getByName() {
            Role role = createRole(UUID.randomUUID(), "USER");
            when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(role));

            RoleResponse result = roleService.getRoleByName("USER");

            assertThat(result.roleName()).isEqualTo("USER");
        }

        @Test
        @DisplayName("getAllRoles — should return all roles")
        void getAll() {
            List<Role> roles = List.of(
                createRole(UUID.randomUUID(), "USER"),
                createRole(UUID.randomUUID(), "ADMIN")
            );
            when(roleRepository.findAll()).thenReturn(roles);

            List<RoleResponse> result = roleService.getAllRoles();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(RoleResponse::roleName)
                .containsExactlyInAnyOrder("USER", "ADMIN");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  createRole
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createRole()")
    class CreateRole {

        @Test
        @DisplayName("Should create role with permissions")
        void shouldCreateWithPermissions() {
            String name = "MODERATOR";
            Set<String> permNames = Set.of("user:read", "profile:read");

            Permission p1 = createPermission(UUID.randomUUID(), "user:read");
            Permission p2 = createPermission(UUID.randomUUID(), "profile:read");

            when(roleRepository.existsByRoleName(name)).thenReturn(false);
            when(permissionRepository.findByName("user:read")).thenReturn(Optional.of(p1));
            when(permissionRepository.findByName("profile:read")).thenReturn(Optional.of(p2));
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> {
                Role r = inv.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });

            RoleResponse result = roleService.createRole(name, "desc", permNames);

            assertThat(result.roleName()).isEqualTo("MODERATOR");
            assertThat(result.permissions()).hasSize(2);
        }

        @Test
        @DisplayName("Should throw when role already exists")
        void shouldThrowWhenDuplicate() {
            when(roleRepository.existsByRoleName("ADMIN")).thenReturn(true);

            assertThatThrownBy(() -> roleService.createRole("ADMIN", "desc", Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

            verify(roleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when permission not found")
        void shouldThrowWhenPermissionNotFound() {
            when(roleRepository.existsByRoleName("NEW_ROLE")).thenReturn(false);
            when(permissionRepository.findByName("unknown:perm")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.createRole("NEW_ROLE", "desc", Set.of("unknown:perm")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Permission not found");
        }

        @Test
        @DisplayName("Should create role with empty permissions")
        void shouldCreateWithEmptyPermissions() {
            when(roleRepository.existsByRoleName("EMPTY")).thenReturn(false);
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> {
                Role r = inv.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });

            RoleResponse result = roleService.createRole("EMPTY", "desc", Set.of());

            assertThat(result.roleName()).isEqualTo("EMPTY");
            assertThat(result.permissions()).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  updateRole
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateRole()")
    class UpdateRole {

        @Test
        @DisplayName("Should update description")
        void shouldUpdateDescription() {
            UUID id = UUID.randomUUID();
            Role role = createRole(id, "ADMIN");

            when(roleRepository.findById(id)).thenReturn(Optional.of(role));
            when(roleRepository.save(any(Role.class))).thenReturn(role);

            RoleResponse result = roleService.updateRole(id, "New desc", null);

            assertThat(result.description()).isEqualTo("New desc");
        }

        @Test
        @DisplayName("Should update permissions")
        void shouldUpdatePermissions() {
            UUID id = UUID.randomUUID();
            Role role = createRole(id, "ADMIN");
            Permission p = createPermission(UUID.randomUUID(), "user:read");

            when(roleRepository.findById(id)).thenReturn(Optional.of(role));
            when(permissionRepository.findByName("user:read")).thenReturn(Optional.of(p));
            when(roleRepository.save(any(Role.class))).thenReturn(role);

            RoleResponse result = roleService.updateRole(id, null, Set.of("user:read"));

            assertThat(result.permissions()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw when role not found")
        void shouldThrowWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(roleRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.updateRole(id, "desc", null))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  deleteRole
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteRole()")
    class DeleteRole {

        @Test
        @DisplayName("Should delete custom role")
        void shouldDeleteCustomRole() {
            UUID id = UUID.randomUUID();
            Role role = createRole(id, "MODERATOR");
            when(roleRepository.findById(id)).thenReturn(Optional.of(role));

            roleService.deleteRole(id);

            verify(roleRepository).delete(role);
        }

        @Test
        @DisplayName("Should reject deleting USER (built-in)")
        void shouldRejectUser() {
            UUID id = UUID.randomUUID();
            Role role = createRole(id, "USER");
            when(roleRepository.findById(id)).thenReturn(Optional.of(role));

            assertThatThrownBy(() -> roleService.deleteRole(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete built-in role: USER");

            verify(roleRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should reject deleting ADMIN (built-in)")
        void shouldRejectAdmin() {
            UUID id = UUID.randomUUID();
            Role role = createRole(id, "ADMIN");
            when(roleRepository.findById(id)).thenReturn(Optional.of(role));

            assertThatThrownBy(() -> roleService.deleteRole(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete built-in role: ADMIN");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  assignRoleToUser / removeRoleFromUser
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Role Assignment")
    class RoleAssignment {

        @Test
        @DisplayName("assignRoleToUser — should add role to user")
        void shouldAssign() {
            UUID userId = UUID.randomUUID();
            UUID roleId = UUID.randomUUID();
            User user = createUser(userId, "test@test.com", Set.of());
            Role role = createRole(roleId, "MODERATOR");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
            when(userRepository.save(any(User.class))).thenReturn(user);

            UserResponse result = roleService.assignRoleToUser(userId, roleId);

            assertThat(result.roles()).hasSize(1);
            assertThat(result.roles().iterator().next().roleName()).isEqualTo("MODERATOR");
        }

        @Test
        @DisplayName("assignRoleToUser — should throw when user not found")
        void assignUserNotFound() {
            UUID userId = UUID.randomUUID();
            UUID roleId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.assignRoleToUser(userId, roleId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("removeRoleFromUser — should remove role")
        void shouldRemove() {
            UUID userId = UUID.randomUUID();
            UUID roleId = UUID.randomUUID();
            Role role = createRole(roleId, "MODERATOR");
            User user = createUser(userId, "test@test.com", Set.of(role));

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
            when(userRepository.save(any(User.class))).thenReturn(user);

            UserResponse result = roleService.removeRoleFromUser(userId, roleId);

            assertThat(result.roles()).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  getUserRoles
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getUserRoles()")
    class GetUserRoles {

        @Test
        @DisplayName("Should return all roles of user")
        void shouldReturnRoles() {
            UUID userId = UUID.randomUUID();
            Set<Role> roles = Set.of(
                createRole(UUID.randomUUID(), "ADMIN"),
                createRole(UUID.randomUUID(), "USER")
            );
            User user = createUser(userId, "test@test.com", roles);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            Set<RoleResponse> result = roleService.getUserRoles(userId);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(RoleResponse::roleName)
                .containsExactlyInAnyOrder("ADMIN", "USER");
        }

        @Test
        @DisplayName("Should throw when user not found")
        void userNotFound() {
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.getUserRoles(userId))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}