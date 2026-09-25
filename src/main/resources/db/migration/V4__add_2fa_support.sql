-- ═══════════════════════════════════════════════════════════════
--  V4 — Two-Factor Authentication (TOTP)
--  Adds 2FA fields to users + creates backup_codes table
-- ═══════════════════════════════════════════════════════════════

-- 1. Add 2FA columns to users
ALTER TABLE users
    ADD COLUMN totp_secret VARCHAR(255) NULL,
    ADD COLUMN two_factor_enabled BIT NOT NULL DEFAULT 0;

-- 2. Create backup_codes table
CREATE TABLE backup_codes (
    id         BINARY(16)   NOT NULL,
    user_id    BINARY(16)   NOT NULL,
    code_hash  VARCHAR(255) NOT NULL,
    used       BIT          NOT NULL DEFAULT 0,
    used_at    DATETIME(6)  NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_backup_codes_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_backup_codes_user ON backup_codes (user_id);