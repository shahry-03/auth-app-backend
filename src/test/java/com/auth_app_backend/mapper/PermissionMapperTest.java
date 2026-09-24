package com.auth_app_backend.mapper;

import com.auth_app_backend.dto.response.PermissionResponse;
import com.auth_app_backend.entity.Permission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PermissionMapper Tests")
class PermissionMapperTest {

    @Test
    void toResponse_nullInput_returnsNull() {
        assertThat(PermissionMapper.toResponse(null)).isNull();
    }

    @Test
    void toResponse_mapsAllFields() {
        Permission p = Permission.builder()
            .id(UUID.randomUUID())
            .name("user:read")
            .description("Read users")
            .build();

        PermissionResponse result = PermissionMapper.toResponse(p);

        assertThat(result.id()).isEqualTo(p.getId());
        assertThat(result.name()).isEqualTo("user:read");
        assertThat(result.description()).isEqualTo("Read users");
    }

    @Test
    void toResponseSet_nullReturnsEmpty() {
        assertThat(PermissionMapper.toResponseSet(null)).isEmpty();
    }

    @Test
    void toResponseSet_emptyReturnsEmpty() {
        assertThat(PermissionMapper.toResponseSet(Set.of())).isEmpty();
    }

    @Test
    void toResponseSet_mapsAll() {
        Permission p1 = Permission.builder().id(UUID.randomUUID()).name("a").build();
        Permission p2 = Permission.builder().id(UUID.randomUUID()).name("b").build();

        Set<PermissionResponse> result = PermissionMapper.toResponseSet(Set.of(p1, p2));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PermissionResponse::name)
            .containsExactlyInAnyOrder("a", "b");
    }
}