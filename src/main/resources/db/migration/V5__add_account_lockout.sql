-- ═══════════════════════════════════════════════════════════════
--  V5 — Account Lockout
--  Adds failed login tracking to prevent brute force
-- ═══════════════════════════════════════════════════════════════

ALTER TABLE users
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until DATETIME(6) NULL,
    ADD COLUMN last_failed_login DATETIME(6) NULL;

-- Index for admin queries (find locked accounts)
CREATE INDEX idx_users_locked_until ON users (locked_until);