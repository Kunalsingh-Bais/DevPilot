package com.repository.service.github;

import com.fasterxml.jackson.annotation.JsonProperty;

// Data we need or received from GitHub repository
public record GithubRepository (
        String repoName,
        String description,
        String language,
        String visibility,
        @JsonProperty("default_branch") String defaultBranch,
        Owner githubOwner
) {

    public record Owner(String login) {
    }
}
