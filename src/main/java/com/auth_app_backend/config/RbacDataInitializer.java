package com.auth_app_backend.config;

import com.auth_app_backend.entity.Permission;
import com.auth_app_backend.entity.Role;
import com.auth_app_backend.repositories.PermissionRepository;
import com.auth_app_backend.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RbacDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public void run(String... args) {

        // ---- Base Permissions ----
        Permission userRead     = createPermission("user:read",     "Read user details");
        Permission userWrite    = createPermission("user:write",    "Create/update users");
        Permission userDelete   = createPermission("user:delete",   "Delete users");
        Permission profileRead  = createPermission("profile:read",  "Read own profile");
        Permission profileWrite = createPermission("profile:write", "Update own profile");
        Permission roleManage   = createPermission("role:manage",   "Manage roles and permissions");

        // ---- Base Roles ----
        createRole("USER", "Default user role — read/update own profile",
            Set.of(profileRead, profileWrite));

        createRole("ADMIN", "Administrator — full user management",
            Set.of(userRead, userWrite, userDelete,
                   profileRead, profileWrite, roleManage));

        log.info("✅ RBAC base data initialized successfully");
    }

    private Permission createPermission(String name, String desc) {
        return permissionRepository.findByName(name)
            .orElseGet(() -> permissionRepository.save(
                Permission.builder()
                    .name(name)
                    .description(desc)
                    .build()));
    }

    private Role createRole(String name, String desc, Set<Permission> perms) {
        return roleRepository.findByRoleName(name)
            .orElseGet(() -> roleRepository.save(
                Role.builder()
                    .roleName(name)
                    .description(desc)
                    .permissions(new HashSet<>(perms))
                    .build()));
    }
}