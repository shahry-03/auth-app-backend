package com.auth_app_backend.mapper;

import com.auth_app_backend.dto.response.RoleResponse;
import com.auth_app_backend.entity.Permission;
import com.auth_app_backend.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RoleMapper Tests")
class RoleMapperTest {

    @Test
    void toResponse_nullInput_returnsNull() {
        assertThat(RoleMapper.toResponse(null)).isNull();
    }

    @Test
    void toResponse_includesPermissions() {
        Permission p = Permission.builder().id(UUID.randomUUID()).name("user:read").build();
        Role role = Role.builder()
            .id(UUID.randomUUID())
            .roleName("ADMIN")
            .description("Admin role")
            .permissions(new HashSet<>(Set.of(p)))
            .build();

        RoleResponse result = RoleMapper.toResponse(role);

        assertThat(result.roleName()).isEqualTo("ADMIN");
        assertThat(result.permissions()).hasSize(1);
    }

    @Test
    void toSummary_excludesPermissions() {
        Permission p = Permission.builder().id(UUID.randomUUID()).name("x").build();
        Role role = Role.builder()
            .id(UUID.randomUUID())
            .roleName("MOD")
            .permissions(new HashSet<>(Set.of(p)))
            .build();

        RoleResponse result = RoleMapper.toSummary(role);

        assertThat(result.roleName()).isEqualTo("MOD");
        assertThat(result.permissions()).isEmpty();
    }

    @Test
    void toResponseSet_nullReturnsEmpty() {
        assertThat(RoleMapper.toResponseSet(null)).isEmpty();
    }

    @Test
    void toSummarySet_nullReturnsEmpty() {
        assertThat(RoleMapper.toSummarySet(null)).isEmpty();
    }
}