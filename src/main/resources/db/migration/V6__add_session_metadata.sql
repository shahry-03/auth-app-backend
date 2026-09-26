-- ═══════════════════════════════════════════════════════════════
--  V6 — Session Metadata
--  Adds device info to refresh_tokens for session management
-- ═══════════════════════════════════════════════════════════════

ALTER TABLE refresh_tokens
    ADD COLUMN ip_address VARCHAR(45) NULL,
    ADD COLUMN user_agent VARCHAR(500) NULL,
    ADD COLUMN last_used_at DATETIME(6) NULL;

-- Optimized index for "active sessions by user" query
CREATE INDEX idx_refresh_tokens_active_sessions
    ON refresh_tokens (user_id, revoked, expires_at);