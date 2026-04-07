-- Phase 2.1: user-scoped GitHub repository tracking with soft delete.
-- Evolves repositories from V1 (owner_id, global github_repo_id uniqueness) while preserving id for child FKs.

ALTER TABLE repositories
    RENAME COLUMN owner_id TO user_id;

ALTER TABLE repositories
    DROP CONSTRAINT IF EXISTS repositories_github_repo_id_key;

ALTER TABLE repositories
    ADD COLUMN full_name VARCHAR(255),
    ADD COLUMN owner_login VARCHAR(255),
    ADD COLUMN is_private BOOLEAN,
    ADD COLUMN html_url TEXT,
    ADD COLUMN updated_at TIMESTAMP,
    ADD COLUMN deleted_at TIMESTAMP;

UPDATE repositories
SET
    full_name = name,
    owner_login = '',
    is_private = FALSE,
    html_url = '',
    updated_at = COALESCE(created_at, CURRENT_TIMESTAMP)
WHERE full_name IS NULL;

ALTER TABLE repositories
    ALTER COLUMN full_name SET NOT NULL,
    ALTER COLUMN owner_login SET NOT NULL,
    ALTER COLUMN is_private SET NOT NULL,
    ALTER COLUMN html_url SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL,
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE repositories
    ADD CONSTRAINT repositories_user_github_repo_unique UNIQUE (user_id, github_repo_id);

CREATE INDEX idx_repositories_user_id ON repositories (user_id);

CREATE INDEX idx_repositories_github_repo_id ON repositories (github_repo_id);
