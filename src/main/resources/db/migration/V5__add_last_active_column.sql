-- V5: Add last_active_at column to users table
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMP WITH TIME ZONE;

-- Add index on last_active_at for efficient online/active status queries
CREATE INDEX IF NOT EXISTS idx_users_last_active
    ON users (last_active_at)
    WHERE deleted_at IS NULL;
