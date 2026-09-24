package com.auth_app_backend.service;

import com.auth_app_backend.dto.response.PermissionResponse;
import com.auth_app_backend.entity.Permission;
import com.auth_app_backend.exception.ResourceNotFoundException;
import com.auth_app_backend.repositories.PermissionRepository;
import com.auth_app_backend.services.impl.PermissionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionService Tests")
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    // ═══════════════════════════════════════════════════════════
    //  Fixtures
    // ═══════════════════════════════════════════════════════════

    private Permission createPermission(UUID id, String name, String desc) {
        return Permission.builder()
            .id(id)
            .name(name)
            .description(desc)
            .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  getPermissionById
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getPermissionById()")
    class GetPermissionById {

        @Test
        @DisplayName("Should return permission when found")
        void shouldReturnPermission() {
            // given
            UUID id = UUID.randomUUID();
            Permission permission = createPermission(id, "user:read", "Read users");
            when(permissionRepository.findById(id)).thenReturn(Optional.of(permission));

            // when
            PermissionResponse result = permissionService.getPermissionById(id);

            // then
            assertThat(result.id()).isEqualTo(id);
            assertThat(result.name()).isEqualTo("user:read");
            assertThat(result.description()).isEqualTo("Read users");
        }

        @Test
        @DisplayName("Should throw when not found")
        void shouldThrowWhenNotFound() {
            // given
            UUID id = UUID.randomUUID();
            when(permissionRepository.findById(id)).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> permissionService.getPermissionById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Permission not found");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  getAllPermissions
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAllPermissions()")
    class GetAllPermissions {

        @Test
        @DisplayName("Should return all permissions")
        void shouldReturnAll() {
            // given
            List<Permission> permissions = List.of(
                createPermission(UUID.randomUUID(), "user:read", "Read users"),
                createPermission(UUID.randomUUID(), "user:write", "Write users"),
                createPermission(UUID.randomUUID(), "role:manage", "Manage roles")
            );
            when(permissionRepository.findAll()).thenReturn(permissions);

            // when
            List<PermissionResponse> result = permissionService.getAllPermissions();

            // then
            assertThat(result).hasSize(3);
            assertThat(result)
                .extracting(PermissionResponse::name)
                .containsExactlyInAnyOrder("user:read", "user:write", "role:manage");
        }

        @Test
        @DisplayName("Should return empty list when none exist")
        void shouldReturnEmptyList() {
            // given
            when(permissionRepository.findAll()).thenReturn(List.of());

            // when
            List<PermissionResponse> result = permissionService.getAllPermissions();

            // then
            assertThat(result).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  createPermission
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createPermission()")
    class CreatePermission {

        @Test
        @DisplayName("Should create permission successfully")
        void shouldCreate() {
            // given
            String name = "product:read";
            String desc = "Read products";
            when(permissionRepository.existsByName(name)).thenReturn(false);
            when(permissionRepository.save(any(Permission.class)))
                .thenAnswer(invocation -> {
                    Permission p = invocation.getArgument(0);
                    p.setId(UUID.randomUUID());
                    return p;
                });

            // when
            PermissionResponse result = permissionService.createPermission(name, desc);

            // then
            assertThat(result.name()).isEqualTo(name);
            assertThat(result.description()).isEqualTo(desc);
            assertThat(result.id()).isNotNull();

            // verify
            ArgumentCaptor<Permission> captor = ArgumentCaptor.forClass(Permission.class);
            verify(permissionRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo(name);
        }

        @Test
        @DisplayName("Should throw when permission already exists")
        void shouldThrowWhenDuplicate() {
            // given
            String name = "user:read";
            when(permissionRepository.existsByName(name)).thenReturn(true);

            // when/then
            assertThatThrownBy(() -> permissionService.createPermission(name, "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

            verify(permissionRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  updatePermission
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updatePermission()")
    class UpdatePermission {

        @Test
        @DisplayName("Should update description when provided")
        void shouldUpdateDescription() {
            // given
            UUID id = UUID.randomUUID();
            Permission permission = createPermission(id, "user:read", "Old desc");
            when(permissionRepository.findById(id)).thenReturn(Optional.of(permission));
            when(permissionRepository.save(any(Permission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            PermissionResponse result = permissionService.updatePermission(id, "New desc");

            // then
            assertThat(result.description()).isEqualTo("New desc");
        }

        @Test
        @DisplayName("Should not update description when null")
        void shouldNotUpdateWhenNull() {
            // given
            UUID id = UUID.randomUUID();
            Permission permission = createPermission(id, "user:read", "Original");
            when(permissionRepository.findById(id)).thenReturn(Optional.of(permission));
            when(permissionRepository.save(any(Permission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            PermissionResponse result = permissionService.updatePermission(id, null);

            // then
            assertThat(result.description()).isEqualTo("Original");
        }

        @Test
        @DisplayName("Should throw when permission not found")
        void shouldThrowWhenNotFound() {
            // given
            UUID id = UUID.randomUUID();
            when(permissionRepository.findById(id)).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> permissionService.updatePermission(id, "desc"))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  deletePermission
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deletePermission()")
    class DeletePermission {

        @Test
        @DisplayName("Should delete permission when exists")
        void shouldDelete() {
            // given
            UUID id = UUID.randomUUID();
            when(permissionRepository.existsById(id)).thenReturn(true);

            // when
            permissionService.deletePermission(id);

            // then
            verify(permissionRepository).deleteById(id);
        }

        @Test
        @DisplayName("Should throw when permission not found")
        void shouldThrowWhenNotFound() {
            // given
            UUID id = UUID.randomUUID();
            when(permissionRepository.existsById(id)).thenReturn(false);

            // when/then
            assertThatThrownBy(() -> permissionService.deletePermission(id))
                .isInstanceOf(ResourceNotFoundException.class);

            verify(permissionRepository, never()).deleteById(any());
        }
    }
}