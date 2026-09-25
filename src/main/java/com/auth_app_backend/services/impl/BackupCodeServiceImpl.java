package com.auth_app_backend.services.impl;

import com.auth_app_backend.entity.BackupCode;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.repositories.BackupCodeRepository;
import com.auth_app_backend.services.BackupCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupCodeServiceImpl implements BackupCodeService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // No 0/O/1/I
    private static final int CODE_LENGTH = 12;   // 12 chars = ~59 bits entropy
    private static final int GROUP_SIZE = 4;     // AAAA-BBBB-CCCC

    private final BackupCodeRepository backupCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public List<String> generateCodes(User user, int count) {
        // 1. Invalidate existing codes
        backupCodeRepository.deleteByUserId(user.getId());

        // 2. Generate new codes
        List<String> plainCodes = new ArrayList<>(count);
        List<BackupCode> entities = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            String plain = generateCode();
            plainCodes.add(plain);

            entities.add(BackupCode.builder()
                .user(user)
                .codeHash(passwordEncoder.encode(normalize(plain)))
                .used(false)
                .build());
        }

        backupCodeRepository.saveAll(entities);
        log.info("Generated {} backup codes for user: {}", count, user.getId());

        return plainCodes;
    }

    @Override
    @Transactional
    public boolean verifyAndConsume(User user, String code) {
        if (code == null || code.isBlank()) return false;

        String normalized = normalize(code);
        List<BackupCode> available = backupCodeRepository.findByUserIdAndUsedFalse(user.getId());

        for (BackupCode bc : available) {
            if (passwordEncoder.matches(normalized, bc.getCodeHash())) {
                bc.setUsed(true);
                bc.setUsedAt(Instant.now());
                backupCodeRepository.save(bc);
                log.info("Backup code consumed for user: {}", user.getId());
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public long countRemaining(User user) {
        return backupCodeRepository.countByUserIdAndUsedFalse(user.getId());
    }

    @Override
    @Transactional
    public void invalidateAll(User user) {
        backupCodeRepository.deleteByUserId(user.getId());
        log.info("All backup codes invalidated for user: {}", user.getId());
    }

    // ─────────────────────────────────────────────────────────

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH + 2);

        for (int i = 0; i < CODE_LENGTH; i++) {
            if (i > 0 && i % GROUP_SIZE == 0) {
                sb.append('-');
            }
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }

        return sb.toString();  // Format: AAAA-BBBB-CCCC
    }

    private String normalize(String code) {
        return code.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }
}