package com.auth_app_backend.mapper;

import com.auth_app_backend.dto.internal.InternalUserResponse;
import com.auth_app_backend.dto.response.UserResponse;
import com.auth_app_backend.entity.Permission;
import com.auth_app_backend.entity.Provider;
import com.auth_app_backend.entity.Role;
import com.auth_app_backend.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserMapper Tests")
class UserMapperTest {

    @Test
    void toResponse_nullInput_returnsNull() {
        assertThat(UserMapper.toResponse(null)).isNull();
    }

    @Test
    void toResponse_mapsBasicFields_neverIncludesPassword() {
        User user = User.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .name("Test User")
            .password("hashed-secret")
            .enabled(true)
            .provider(Provider.LOCAL)
            .roles(new HashSet<>())
            .build();

        UserResponse result = UserMapper.toResponse(user);

        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.name()).isEqualTo("Test User");
        assertThat(result.enabled()).isTrue();
        // UserResponse has no password field — compile-time guarantee
    }

    @Test
    void toResponse_withPermissions_includesNestedPermissions() {
        Permission p = Permission.builder().id(UUID.randomUUID()).name("user:read").build();
        Role role = Role.builder().id(UUID.randomUUID()).roleName("ADMIN")
            .permissions(new HashSet<>(Set.of(p))).build();
        User user = User.builder().id(UUID.randomUUID()).email("a@b.com")
            .roles(new HashSet<>(Set.of(role))).build();

        UserResponse result = UserMapper.toResponse(user, true);

        assertThat(result.roles()).hasSize(1);
        assertThat(result.roles().iterator().next().permissions()).hasSize(1);
    }

    @Test
    void toResponse_withoutPermissions_usesSummary() {
        Permission p = Permission.builder().id(UUID.randomUUID()).name("user:read").build();
        Role role = Role.builder().id(UUID.randomUUID()).roleName("ADMIN")
            .permissions(new HashSet<>(Set.of(p))).build();
        User user = User.builder().id(UUID.randomUUID()).email("a@b.com")
            .roles(new HashSet<>(Set.of(role))).build();

        UserResponse result = UserMapper.toResponse(user, false);

        assertThat(result.roles()).hasSize(1);
        assertThat(result.roles().iterator().next().permissions()).isEmpty();
    }

    @Test
    void toInternalResponse_buildsAuthorities() {
        Permission p1 = Permission.builder().id(UUID.randomUUID()).name("user:read").build();
        Permission p2 = Permission.builder().id(UUID.randomUUID()).name("user:write").build();
        Role admin = Role.builder().id(UUID.randomUUID()).roleName("ADMIN")
            .permissions(new HashSet<>(Set.of(p1, p2))).build();

        User user = User.builder()
            .id(UUID.randomUUID())
            .email("admin@test.com")
            .password("hashed")
            .enabled(true)
            .provider(Provider.LOCAL)
            .roles(new HashSet<>(Set.of(admin)))
            .build();

        InternalUserResponse result = UserMapper.toInternalResponse(user);

        assertThat(result.email()).isEqualTo("admin@test.com");
        assertThat(result.password()).isEqualTo("hashed"); // internal only
        assertThat(result.authorities())
            .contains("ROLE_ADMIN", "user:read", "user:write");
    }
}