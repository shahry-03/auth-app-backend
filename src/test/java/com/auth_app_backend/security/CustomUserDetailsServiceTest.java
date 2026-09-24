package com.auth_app_backend.security;

import com.auth_app_backend.entity.User;
import com.auth_app_backend.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("Should load user by email")
    void shouldLoadUserByEmail() {
        User user = User.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .name("Test")
            .password("hashed")
            .enabled(true)
            .roles(new HashSet<>())
            .build();

        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("test@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("test@example.com");
        assertThat(result.getPassword()).isEqualTo("hashed");
        assertThat(result.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when not found")
    void shouldThrowWhenNotFound() {
        when(userRepository.findByEmail("ghost@example.com"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            userDetailsService.loadUserByUsername("ghost@example.com"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("User not found: ghost@example.com");
    }

    @Test
    @DisplayName("Should return User entity as UserDetails")
    void shouldReturnUserAsUserDetails() {
        User user = User.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .password("hashed")
            .enabled(true)
            .roles(new HashSet<>())
            .build();

        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("test@example.com");

        assertThat(result).isInstanceOf(User.class);
    }
}