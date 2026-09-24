-- ═══════════════════════════════════════════════════════════════
--  V3 — Email Verification Support
--  Adds email_verified to users + creates verification_tokens
-- ═══════════════════════════════════════════════════════════════

-- 1. Add email_verified column to users
ALTER TABLE users
    ADD COLUMN email_verified BIT NOT NULL DEFAULT 0;

-- 2. Create verification_tokens table
CREATE TABLE verification_tokens (
    id         BINARY(16)   NOT NULL,
    token      VARCHAR(255) NOT NULL,
    user_id    BINARY(16)   NOT NULL,
    type       VARCHAR(50)  NOT NULL,
    expires_at DATETIME(6)  NOT NULL,
    used       BIT          NOT NULL DEFAULT 0,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_verification_tokens_token UNIQUE (token),
    CONSTRAINT fk_verification_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_verification_tokens_user  ON verification_tokens (user_id);
CREATE INDEX idx_verification_tokens_token ON verification_tokens (token);