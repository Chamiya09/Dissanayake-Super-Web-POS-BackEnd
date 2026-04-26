-- Add contact details for user profiles and Add User form submissions.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(30),
    ADD COLUMN IF NOT EXISTS address VARCHAR(255);
