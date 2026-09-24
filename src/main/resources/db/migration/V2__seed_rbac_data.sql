-- ═══════════════════════════════════════════════════════════════
--  V2 — Seed RBAC Base Data
--  Same data as RbacDataInitializer — idempotent (INSERT IGNORE)
-- ═══════════════════════════════════════════════════════════════

-- ─── Base Permissions ──────────────────────────────────────────
INSERT IGNORE INTO permissions (permission_id, permission_name, description) VALUES
    (UUID_TO_BIN(UUID()), 'user:read',     'Read user details'),
    (UUID_TO_BIN(UUID()), 'user:write',    'Create/update users'),
    (UUID_TO_BIN(UUID()), 'user:delete',   'Delete users'),
    (UUID_TO_BIN(UUID()), 'profile:read',  'Read own profile'),
    (UUID_TO_BIN(UUID()), 'profile:write', 'Update own profile'),
    (UUID_TO_BIN(UUID()), 'role:manage',   'Manage roles and permissions');

-- ─── Base Roles ────────────────────────────────────────────────
INSERT IGNORE INTO roles (role_id, role_name, description) VALUES
    (UUID_TO_BIN(UUID()), 'USER',  'Default user role — read/update own profile'),
    (UUID_TO_BIN(UUID()), 'ADMIN', 'Administrator — full user management');

-- ─── USER role → profile:read, profile:write ───────────────────
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.permission_name IN ('profile:read', 'profile:write')
WHERE r.role_name = 'USER';

-- ─── ADMIN role → all 6 permissions ────────────────────────────
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ADMIN';