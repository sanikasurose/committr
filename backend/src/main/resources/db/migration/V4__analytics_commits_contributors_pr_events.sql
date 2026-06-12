-- Phase 3: analytics columns for commits + contributors, and new pr_events table.

-- commits: track line-level diff stats and primary language per commit
ALTER TABLE commits
    ADD COLUMN lines_added   INT NOT NULL DEFAULT 0,
    ADD COLUMN lines_deleted INT NOT NULL DEFAULT 0,
    ADD COLUMN language      VARCHAR(100);

-- contributors: GitHub contributors are often not Committr users, so user_id
-- must become nullable. Drop the existing NOT NULL unique constraint first,
-- then relax the column, then add identity columns for external contributors.
ALTER TABLE contributors
    DROP CONSTRAINT contributors_user_id_repo_id_key;

ALTER TABLE contributors
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE contributors
    ADD COLUMN github_login      VARCHAR(255),
    ADD COLUMN github_avatar_url TEXT;

-- pr_events: one row per pull request, tracks open→merge lifecycle
-- for PR velocity calculation in SnapshotService.
CREATE TABLE pr_events (
    id        BIGSERIAL PRIMARY KEY,
    repo_id   BIGINT NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    pr_number INT    NOT NULL,
    opened_at TIMESTAMP NOT NULL,
    merged_at TIMESTAMP,
    UNIQUE (repo_id, pr_number)
);

CREATE INDEX idx_pr_events_repo_merged ON pr_events(repo_id, merged_at);
CREATE INDEX idx_commits_repo_id ON commits(repo_id);
