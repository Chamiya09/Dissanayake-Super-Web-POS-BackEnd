-- Add role-based login/member ID for manager and staff users.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS member_id VARCHAR(30);

-- Enforce uniqueness for non-null values.
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_member_id ON users(member_id);
