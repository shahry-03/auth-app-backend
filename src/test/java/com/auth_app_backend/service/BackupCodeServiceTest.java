package com.auth_app_backend.service;

import com.auth_app_backend.entity.BackupCode;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.repositories.BackupCodeRepository;
import com.auth_app_backend.services.impl.BackupCodeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BackupCodeService Tests")
class BackupCodeServiceTest {

    @Mock private BackupCodeRepository backupCodeRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private BackupCodeServiceImpl backupCodeService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .name("Test User")
            .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  generateCodes
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("generateCodes()")
    class GenerateCodes {

        @Test
        @DisplayName("Should generate requested number of codes")
        void shouldGenerateRequestedCount() {
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(backupCodeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            List<String> codes = backupCodeService.generateCodes(testUser, 10);

            assertThat(codes).hasSize(10);
        }

        @Test
        @DisplayName("Should delete existing codes before generating new ones")
        void shouldDeleteExistingCodes() {
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(backupCodeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            backupCodeService.generateCodes(testUser, 10);

            verify(backupCodeRepository).deleteByUserId(testUser.getId());
        }

        @Test
        @DisplayName("Should generate unique codes")
        void shouldGenerateUniqueCodes() {
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(backupCodeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            List<String> codes = backupCodeService.generateCodes(testUser, 10);

            assertThat(codes).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("Should generate codes in correct format (XXXX-XXXX-XXXX)")
        void shouldGenerateCorrectFormat() {
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(backupCodeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            List<String> codes = backupCodeService.generateCodes(testUser, 5);

            codes.forEach(code -> {
                assertThat(code).matches("^[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$");
                assertThat(code).hasSize(14); // 12 chars + 2 dashes
            });
        }

        @Test
        @DisplayName("Should hash codes before saving")
        void shouldHashCodes() {
            when(passwordEncoder.encode(anyString())).thenReturn("bcrypt-hash");
            when(backupCodeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            backupCodeService.generateCodes(testUser, 3);

            ArgumentCaptor<List<BackupCode>> captor = ArgumentCaptor.forClass(List.class);
            verify(backupCodeRepository).saveAll(captor.capture());

            List<BackupCode> savedCodes = captor.getValue();
            assertThat(savedCodes).hasSize(3);
            savedCodes.forEach(bc -> {
                assertThat(bc.getCodeHash()).isEqualTo("bcrypt-hash");
                assertThat(bc.isUsed()).isFalse();
                assertThat(bc.getUser()).isEqualTo(testUser);
            });
        }

        @Test
        @DisplayName("Should hash normalized code (no dashes, uppercase)")
        void shouldHashNormalizedCode() {
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(backupCodeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            backupCodeService.generateCodes(testUser, 1);

            // Verify that passwordEncoder.encode was called with normalized string
            // (no dashes, uppercase, 12 chars)
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(passwordEncoder).encode(captor.capture());

            String normalized = captor.getValue();
            assertThat(normalized).hasSize(12);
            assertThat(normalized).matches("^[A-Z0-9]{12}$");
            assertThat(normalized).doesNotContain("-");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  verifyAndConsume
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("verifyAndConsume()")
    class VerifyAndConsume {

        @Test
        @DisplayName("Should return true and mark code as used")
        void shouldVerifyAndConsume() {
            String plainCode = "ABCD-EFGH-IJKL";
            String normalizedCode = "ABCDEFGHIJKL";

            BackupCode storedCode = BackupCode.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .codeHash("$2a$10$hashed")
                .used(false)
                .build();

            when(backupCodeRepository.findByUserIdAndUsedFalse(testUser.getId()))
                .thenReturn(List.of(storedCode));
            when(passwordEncoder.matches(normalizedCode, storedCode.getCodeHash()))
                .thenReturn(true);
            when(backupCodeRepository.save(any(BackupCode.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            boolean result = backupCodeService.verifyAndConsume(testUser, plainCode);

            assertThat(result).isTrue();
            assertThat(storedCode.isUsed()).isTrue();
            assertThat(storedCode.getUsedAt()).isNotNull();
            verify(backupCodeRepository).save(storedCode);
        }

        @Test
        @DisplayName("Should return false when code doesn't match")
        void shouldReturnFalseWhenNoMatch() {
            BackupCode storedCode = BackupCode.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .codeHash("hash")
                .used(false)
                .build();

            when(backupCodeRepository.findByUserIdAndUsedFalse(testUser.getId()))
                .thenReturn(List.of(storedCode));
            when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(false);

            boolean result = backupCodeService.verifyAndConsume(testUser, "WRONG-CODE-XXXX");

            assertThat(result).isFalse();
            assertThat(storedCode.isUsed()).isFalse();
            verify(backupCodeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should return false when null code provided")
        void shouldRejectNullCode() {
            boolean result = backupCodeService.verifyAndConsume(testUser, null);

            assertThat(result).isFalse();
            verify(backupCodeRepository, never()).findByUserIdAndUsedFalse(any());
        }

        @Test
        @DisplayName("Should return false when blank code provided")
        void shouldRejectBlankCode() {
            boolean result = backupCodeService.verifyAndConsume(testUser, "   ");

            assertThat(result).isFalse();
            verify(backupCodeRepository, never()).findByUserIdAndUsedFalse(any());
        }

        @Test
        @DisplayName("Should normalize input code (remove dashes, uppercase)")
        void shouldNormalizeInput() {
            String inputCode = "abcd-efgh-ijkl";    // lowercase + dashes

            BackupCode storedCode = BackupCode.builder()
                .user(testUser)
                .codeHash("hash")
                .used(false)
                .build();

            when(backupCodeRepository.findByUserIdAndUsedFalse(testUser.getId()))
                .thenReturn(List.of(storedCode));
            when(passwordEncoder.matches("ABCDEFGHIJKL", "hash"))    // uppercase + no dashes
                .thenReturn(true);
            when(backupCodeRepository.save(any(BackupCode.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            boolean result = backupCodeService.verifyAndConsume(testUser, inputCode);

            assertThat(result).isTrue();
            verify(passwordEncoder).matches("ABCDEFGHIJKL", "hash");
        }

        @Test
        @DisplayName("Should return false when user has no codes")
        void shouldReturnFalseWhenNoCodes() {
            when(backupCodeRepository.findByUserIdAndUsedFalse(testUser.getId()))
                .thenReturn(List.of());

            boolean result = backupCodeService.verifyAndConsume(testUser, "ABCD-EFGH-IJKL");

            assertThat(result).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  countRemaining
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("countRemaining()")
    class CountRemaining {

        @Test
        @DisplayName("Should return count from repository")
        void shouldReturnCount() {
            when(backupCodeRepository.countByUserIdAndUsedFalse(testUser.getId()))
                .thenReturn(7L);

            long count = backupCodeService.countRemaining(testUser);

            assertThat(count).isEqualTo(7L);
        }

        @Test
        @DisplayName("Should return zero when none remaining")
        void shouldReturnZero() {
            when(backupCodeRepository.countByUserIdAndUsedFalse(testUser.getId()))
                .thenReturn(0L);

            assertThat(backupCodeService.countRemaining(testUser)).isZero();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  invalidateAll
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("invalidateAll()")
    class InvalidateAll {

        @Test
        @DisplayName("Should delete all codes for user")
        void shouldDeleteAll() {
            backupCodeService.invalidateAll(testUser);

            verify(backupCodeRepository).deleteByUserId(testUser.getId());
        }
    }
}