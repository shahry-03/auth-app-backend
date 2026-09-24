-- ═══════════════════════════════════════════════════════════════
--  V1 — Initial Schema
--  Creates all core tables + indexes + foreign keys
--  Matches entities: User, Role, Permission, RefreshToken
-- ═══════════════════════════════════════════════════════════════

-- ─── Permissions ───────────────────────────────────────────────
CREATE TABLE permissions (
    permission_id   BINARY(16)      NOT NULL,
    permission_name VARCHAR(100)    NOT NULL,
    description     VARCHAR(500)    NULL,
    PRIMARY KEY (permission_id),
    CONSTRAINT uk_permissions_name UNIQUE (permission_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Roles ─────────────────────────────────────────────────────
CREATE TABLE roles (
    role_id     BINARY(16)      NOT NULL,
    role_name   VARCHAR(100)    NOT NULL,
    description VARCHAR(500)    NULL,
    PRIMARY KEY (role_id),
    CONSTRAINT uk_roles_name UNIQUE (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Users ─────────────────────────────────────────────────────
CREATE TABLE users (
    user_id     BINARY(16)      NOT NULL,
    user_email  VARCHAR(300)    NULL,
    user_name   VARCHAR(500)    NULL,
    password    VARCHAR(255)    NULL,
    image       VARCHAR(255)    NULL,
    enabled     BIT             NOT NULL DEFAULT 1,
    created_at  DATETIME(6)     NULL,
    updated_at  DATETIME(6)     NULL,
    provider    ENUM('LOCAL','GOOGLE','FACEBOOK','GITHUB') NULL,
    provider_id VARCHAR(255)    NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_users_email UNIQUE (user_email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Refresh Tokens ────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id                BINARY(16)      NOT NULL,
    jti               VARCHAR(255)    NOT NULL,
    user_id           BINARY(16)      NOT NULL,
    created_at        DATETIME(6)     NOT NULL,
    expires_at        DATETIME(6)     NULL,
    revoked           BIT             NOT NULL DEFAULT 0,
    replaced_by_token VARCHAR(255)    NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_jti UNIQUE (jti),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- ─── Junction: user_roles ──────────────────────────────────────
CREATE TABLE user_roles (
    user_id BINARY(16) NOT NULL,
    role_id BINARY(16) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (role_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Junction: role_permissions ────────────────────────────────
CREATE TABLE role_permissions (
    role_id       BINARY(16) NOT NULL,
    permission_id BINARY(16) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles (role_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions (permission_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;