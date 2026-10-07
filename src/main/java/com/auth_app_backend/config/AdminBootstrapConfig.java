package com.auth_app_backend.config;

import com.auth_app_backend.entity.Role;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.repositories.RoleRepository;
import com.auth_app_backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@Order(1)   // RBAC seed (RoleInitializer) ke BAAD chalega
@RequiredArgsConstructor
public class AdminBootstrapConfig implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.enabled:false}")
    private boolean enabled;

    @Value("${app.bootstrap.admin.email:}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password:}")
    private String adminPassword;

    @Value("${app.bootstrap.admin.name:Administrator}")
    private String adminName;

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.debug("Admin bootstrap disabled — skipping");
            return;
        }

        if (adminEmail == null || adminEmail.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            log.warn("Admin bootstrap enabled but email/password not set — skipping");
            return;
        }

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin user already exists: {}", adminEmail);
            return;
        }

        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .orElseThrow(() -> new IllegalStateException(
                        "ADMIN role not found. Check RBAC seed migration."));

        Role userRole = roleRepository.findByRoleName("USER")
                .orElseThrow(() -> new IllegalStateException(
                        "USER role not found. Check RBAC seed migration."));

        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        roles.add(userRole);   // Admin also has USER role

        User admin = User.builder()
                .email(adminEmail)
                .name(adminName)
                .password(passwordEncoder.encode(adminPassword))
                .enabled(true)
                .emailVerified(true)     // ⚡ Verified — no email check
                .roles(roles)
                .build();

        userRepository.save(admin);
        log.info("════════════════════════════════════════════");
        log.info("  BOOTSTRAP ADMIN CREATED");
        log.info("  Email: {}", adminEmail);
        log.info("  Change password immediately after first login!");
        log.info("════════════════════════════════════════════");
    }
}