package com.repository.service.dto;

import com.repository.service.github.GithubRepository;

// The JSON we return to callers.
public record RepositorySummary (
        String githubOwner,
        String repoName,
        String description,
        String language,
        String visibility,
        String defaultBranch
){

    // Copies the useful fields from GitHub's shape into ours
    public static RepositorySummary from(GithubRepository repo) {
        return new RepositorySummary(
                repo.githubOwner().login(),
                repo.repoName(),
                repo.description(),
                repo.language(),
                repo.visibility(),
                repo.defaultBranch()
        );
    }
}
