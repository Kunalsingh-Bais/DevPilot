CREATE EXTENSION IF NOT EXISTS vector;

-- One row per GitHub repository that the user has indexed.
CREATE TABLE repositories (
    id             BIGSERIAL PRIMARY KEY,
    Github_owner   VARCHAR(100) NOT NULL,
    repo_name      VARCHAR(100) NOT NULL,
    default_branch VARCHAR(100),           -- branch used for indexing, e.g. "main"
    commit_sha     VARCHAR(40),            -- exact commit that was indexed
    status         VARCHAR(20) NOT NULL,
    error_message  TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- The same repository must not be stored twice.
    CONSTRAINT uq_repositories_owner_name UNIQUE (Github_owner, repo_name),

    -- Only allow the four known status values.
    CONSTRAINT ck_repositories_status
        CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED'))
);

-- One row per chunk of code (a piece of a file).
CREATE TABLE code_chunks (
    id            BIGSERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    file_path     TEXT NOT NULL,
    start_line    INTEGER NOT NULL,
    end_line      INTEGER NOT NULL,
    content       TEXT NOT NULL,            -- actual code text
    commit_sha    VARCHAR(40) NOT NULL,     -- commit this chunk came from
    embedding     vector(768) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_code_chunks_lines CHECK (start_line >= 1 AND end_line >= start_line)
);

-- Speeds up "give me all chunks for this repository" queries.
CREATE INDEX idx_code_chunks_repository_id ON code_chunks(repository_id);