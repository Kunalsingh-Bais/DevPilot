-- V2: every indexed repository now belongs to one signed-in GitHub user.

-- Step 1: remove the old test rows. Their chunks go too, because of ON DELETE CASCADE in V1.
DELETE FROM repositories;

-- Step 2: add the owner column. It can't be empty.
ALTER TABLE repositories ADD COLUMN user_login VARCHAR(100) NOT NULL;

-- Step 3: the same GitHub repository can now be indexed by several users.
-- Remove the old rule that allowed only one row per GitHub repository.
ALTER TABLE repositories DROP CONSTRAINT IF EXISTS uq_repositories_owner_name;

-- Step 4: one row per user, per GitHub repository.
ALTER TABLE repositories ADD CONSTRAINT uq_repositories_user_owner_name UNIQUE (
        user_login, github_owner, repo_name);

-- Step 5: speed up "show me this user's repositories".
CREATE INDEX idx_repositories_user_login ON repositories(user_login);