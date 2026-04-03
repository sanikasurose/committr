-- OAuth persistence: token column (plaintext placeholder until encryption) and row update time
UPDATE users SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;

ALTER TABLE users ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE users
    ADD COLUMN encrypted_access_token TEXT NOT NULL DEFAULT '';

ALTER TABLE users
    ALTER COLUMN encrypted_access_token DROP DEFAULT;

ALTER TABLE users
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE users
SET updated_at = COALESCE(created_at, CURRENT_TIMESTAMP);

ALTER TABLE users
    ALTER COLUMN updated_at DROP DEFAULT;
