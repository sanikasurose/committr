-- users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    github_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    avatar_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- repositories
CREATE TABLE repositories (
    id BIGSERIAL PRIMARY KEY,
    github_repo_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    owner_id BIGINT NOT NULL
        REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- contributors
CREATE TABLE contributors (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL
        REFERENCES users (id) ON DELETE CASCADE,
    repo_id BIGINT NOT NULL
        REFERENCES repositories (id) ON DELETE CASCADE,
    total_commits INT DEFAULT 0,
    UNIQUE (user_id, repo_id)
);

-- commits
CREATE TABLE commits (
    id BIGSERIAL PRIMARY KEY,
    github_commit_id VARCHAR(255) NOT NULL UNIQUE,
    repo_id BIGINT NOT NULL
        REFERENCES repositories (id) ON DELETE CASCADE,
    contributor_id BIGINT NOT NULL
        REFERENCES contributors (id) ON DELETE CASCADE,
    message TEXT,
    committed_at TIMESTAMP NOT NULL
);

-- snapshots
CREATE TABLE snapshots (
    id BIGSERIAL PRIMARY KEY,
    repo_id BIGINT NOT NULL
        REFERENCES repositories (id) ON DELETE CASCADE,
    contributor_id BIGINT NOT NULL
        REFERENCES contributors (id) ON DELETE CASCADE,
    week_start DATE NOT NULL,
    commit_count INT DEFAULT 0,
    UNIQUE (repo_id, contributor_id, week_start)
);

CREATE INDEX idx_commits_committed_at ON commits (committed_at);

CREATE INDEX idx_snapshots_week_start ON snapshots (week_start);

CREATE INDEX idx_snapshots_repo_contributor_week ON snapshots (repo_id, contributor_id, week_start);
