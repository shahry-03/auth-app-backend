package com.auth_app_backend.service;

import com.auth_app_backend.dto.request.ChangePasswordRequest;
import com.auth_app_backend.dto.request.UpdateUserRequest;
import com.auth_app_backend.dto.response.UserResponse;
import com.auth_app_backend.entity.Role;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.exception.ResourceNotFoundException;
import com.auth_app_backend.repositories.UserRepository;
import com.auth_app_backend.services.RefreshTokenService;
import com.auth_app_backend.services.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
            .id(userId)
            .email("test@example.com")
            .name("Test User")
            .password("hashed-password")
            .image(null)
            .enabled(true)
            .roles(new HashSet<>())
            .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  Read operations
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Read Operations")
    class ReadOperations {

        @Test
        @DisplayName("getUserById — returns user when found")
        void getUserById() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            UserResponse result = userService.getUserById(userId);

            assertThat(result.id()).isEqualTo(userId);
            assertThat(result.email()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("getUserById — throws when not found")
        void getUserByIdNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("getUserByEmail — returns user")
        void getUserByEmail() {
            when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

            UserResponse result = userService.getUserByEmail("test@example.com");

            assertThat(result.email()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("getUserByEmail — throws when not found")
        void getUserByEmailNotFound() {
            when(userRepository.findByEmail("missing@x.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByEmail("missing@x.com"))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("getAllUsers — returns list")
        void getAllUsers() {
            User secondUser = User.builder()
                .id(UUID.randomUUID())
                .email("user2@test.com")
                .name("User 2")
                .password("hash")
                .enabled(true)
                .roles(new HashSet<>())
                .build();
            when(userRepository.findAll()).thenReturn(List.of(testUser, secondUser));

            List<UserResponse> result = userService.getAllUsers();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(UserResponse::email)
                .containsExactlyInAnyOrder("test@example.com", "user2@test.com");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  updateUser
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateUser()")
    class UpdateUser {

        @Test
        @DisplayName("Should update name when provided")
        void shouldUpdateName() {
            UpdateUserRequest request = new UpdateUserRequest("New Name", null, null);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);

            UserResponse result = userService.updateUser(userId, request);

            assertThat(result.name()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("Should update image when provided")
        void shouldUpdateImage() {
            UpdateUserRequest request = new UpdateUserRequest(null, "https://img.com/x.png", null);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);

            UserResponse result = userService.updateUser(userId, request);

            assertThat(result.image()).isEqualTo("https://img.com/x.png");
        }

        @Test
        @DisplayName("Should update enabled flag")
        void shouldUpdateEnabled() {
            UpdateUserRequest request = new UpdateUserRequest(null, null, false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);

            userService.updateUser(userId, request);

            assertThat(testUser.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should leave fields unchanged when null")
        void shouldNotChangeNullFields() {
            UpdateUserRequest request = new UpdateUserRequest(null, null, null);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);

            userService.updateUser(userId, request);

            assertThat(testUser.getName()).isEqualTo("Test User");
            assertThat(testUser.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should throw when user not found")
        void shouldThrowWhenNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                userService.updateUser(userId, new UpdateUserRequest("x", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  deleteUser
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("Should delete when user exists")
        void shouldDelete() {
            when(userRepository.existsById(userId)).thenReturn(true);

            userService.deleteUser(userId);

            verify(userRepository).deleteById(userId);
        }

        @Test
        @DisplayName("Should throw when user not found")
        void shouldThrowWhenNotFound() {
            when(userRepository.existsById(userId)).thenReturn(false);

            assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).deleteById(any());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  changePassword
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("Should change password when current matches")
        void shouldChangePassword() {
            ChangePasswordRequest req = new ChangePasswordRequest("OldPass@123", "NewPass@456");

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("OldPass@123", "hashed-password")).thenReturn(true);
            when(passwordEncoder.encode("NewPass@456")).thenReturn("new-hash");
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);

            userService.changePassword(userId, req);

            assertThat(testUser.getPassword()).isEqualTo("new-hash");
            verify(refreshTokenService).revokeAllForUser(userId);
        }

        @Test
        @DisplayName("Should throw when current password wrong")
        void shouldThrowWhenWrong() {
            ChangePasswordRequest req = new ChangePasswordRequest("WrongPass@123", "NewPass@456");

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("WrongPass@123", "hashed-password")).thenReturn(false);

            assertThatThrownBy(() -> userService.changePassword(userId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("incorrect");

            verify(userRepository, never()).saveAndFlush(any());
            verify(refreshTokenService, never()).revokeAllForUser(any());
        }

        @Test
        @DisplayName("Should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                userService.changePassword(userId, new ChangePasswordRequest("a", "b")))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  enableUser
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("enableUser()")
    class EnableUser {

        @Test
        @DisplayName("Should enable user")
        void shouldEnable() {
            testUser.setEnabled(false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);

            userService.enableUser(userId, true);

            assertThat(testUser.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should disable user")
        void shouldDisable() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);

            userService.enableUser(userId, false);

            assertThat(testUser.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should throw when user not found")
        void shouldThrowWhenNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.enableUser(userId, true))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}