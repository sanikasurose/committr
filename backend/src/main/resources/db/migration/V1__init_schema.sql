-- USERS
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    github_id BIGINT UNIQUE NOT NULL,
    username VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- REPOSITORIES
CREATE TABLE repositories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    owner_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- CONTRIBUTORS
CREATE TABLE contributors (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    repo_id INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (repo_id) REFERENCES repositories(id)
);

-- COMMITS
CREATE TABLE commits (
    id SERIAL PRIMARY KEY,
    repo_id INTEGER NOT NULL,
    contributor_id INTEGER NOT NULL,
    message TEXT,
    committed_at TIMESTAMP NOT NULL,
    FOREIGN KEY (repo_id) REFERENCES repositories(id),
    FOREIGN KEY (contributor_id) REFERENCES contributors(id)
);

-- SNAPSHOTS
CREATE TABLE snapshots (
    id SERIAL PRIMARY KEY,
    repo_id INTEGER NOT NULL,
    contributor_id INTEGER NOT NULL,
    week_start DATE NOT NULL,
    commit_count INTEGER DEFAULT 0,
    FOREIGN KEY (repo_id) REFERENCES repositories(id),
    FOREIGN KEY (contributor_id) REFERENCES contributors(id)
);

-- INDEXES (IMPORTANT)

CREATE INDEX idx_commits_committed_at ON commits(committed_at);

CREATE INDEX idx_snapshots_week_start ON snapshots(week_start);

CREATE INDEX idx_snapshots_composite 
ON snapshots(repo_id, contributor_id, week_start);