-- ─────────────────────────────────────────────────────────────────────────────
-- V5 · Users table
--
-- Stores system users with bcrypt-hashed passwords.
-- Default password for all seed accounts is "password".
-- BCrypt("password", cost=10):  $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE users (
    id                   BIGSERIAL    PRIMARY KEY,
    username             VARCHAR(50)  NOT NULL UNIQUE,
    full_name            VARCHAR(150) NOT NULL,
    email                VARCHAR(150) NOT NULL UNIQUE,
    password_hash        VARCHAR(255) NOT NULL,
    role                 VARCHAR(30)  NOT NULL CHECK (role IN ('Owner','Manager','Staff')),
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    email_notifications  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── Seed the three demo accounts ─────────────────────────────────────────────
INSERT INTO users (username, full_name, email, password_hash, role) VALUES
  ('admin',    'Nuwan Dissanayake', 'nuwan@dissanayake.lk',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Owner'),
  ('manager1', 'Kamala Perera',     'kamala@dissanayake.lk',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Manager'),
  ('staff1',   'Sachini Fernando',  'sachini@dissanayake.lk', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Staff');
