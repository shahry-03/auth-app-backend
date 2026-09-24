package com.auth_app_backend.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserHelper Tests")
class UserHelperTest {

    @Test
    @DisplayName("parseId — should parse valid UUID string")
    void shouldParseValidUuid() {
        UUID expected = UUID.randomUUID();
        UUID result = UserHelper.parseId(expected.toString());
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("parseId — should throw on invalid UUID format")
    void shouldThrowOnInvalidFormat() {
        assertThatThrownBy(() -> UserHelper.parseId("not-a-uuid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid UUID format");
    }

    @Test
    @DisplayName("parseId — should throw on null")
    void shouldThrowOnNull() {
        assertThatThrownBy(() -> UserHelper.parseId(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("null or empty");
    }

    @Test
    @DisplayName("parseId — should throw on blank")
    void shouldThrowOnBlank() {
        assertThatThrownBy(() -> UserHelper.parseId("   "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("parseIdOrNull — returns null for invalid")
    void parseOrNullInvalid() {
        assertThat(UserHelper.parseIdOrNull("bad")).isNull();
        assertThat(UserHelper.parseIdOrNull(null)).isNull();
    }

    @Test
    @DisplayName("parseIdOrNull — parses valid UUID")
    void parseOrNullValid() {
        UUID expected = UUID.randomUUID();
        assertThat(UserHelper.parseIdOrNull(expected.toString())).isEqualTo(expected);
    }
}