ALTER TABLE users
    ADD COLUMN IF NOT EXISTS manager_user_id BIGINT;

ALTER TABLE users
    ADD CONSTRAINT fk_users_manager_user
    FOREIGN KEY (manager_user_id)
    REFERENCES users (id)
    ON DELETE SET NULL;
