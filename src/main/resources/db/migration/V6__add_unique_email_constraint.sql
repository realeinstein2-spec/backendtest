-- V6: Add partial unique index on email column to support social auth uniqueness
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_unique
    ON users (email)
    WHERE deleted_at IS NULL AND email IS NOT NULL;
