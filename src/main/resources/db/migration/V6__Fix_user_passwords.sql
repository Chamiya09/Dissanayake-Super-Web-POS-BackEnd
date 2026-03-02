-- ─────────────────────────────────────────────────────────────────────────────
-- V6 · Fix user password hashes
--
-- The hash stored in V5 was incorrect (it didn't match any intended password).
-- This migration replaces all three seed-account hashes with verified values.
--
-- Passwords:
--   admin    → "admin123"   BCrypt-10: $2a$10$M/HgOD99LcD4I2hgvgONruB2Mjisf51xZ4o0QuzF8llo7GUE/0VvK
--   manager1 → "password"   BCrypt-10: $2a$10$9QGlCFw8nRXCUZlK7lEzM.l/6gy.kAvfyM0sElfidZ9OGMip4MKqC
--   staff1   → "password"   BCrypt-10: $2a$10$9QGlCFw8nRXCUZlK7lEzM.l/6gy.kAvfyM0sElfidZ9OGMip4MKqC
-- ─────────────────────────────────────────────────────────────────────────────

UPDATE users SET password_hash = '$2a$10$M/HgOD99LcD4I2hgvgONruB2Mjisf51xZ4o0QuzF8llo7GUE/0VvK'
WHERE username = 'admin';

UPDATE users SET password_hash = '$2a$10$9QGlCFw8nRXCUZlK7lEzM.l/6gy.kAvfyM0sElfidZ9OGMip4MKqC'
WHERE username IN ('manager1', 'staff1');
