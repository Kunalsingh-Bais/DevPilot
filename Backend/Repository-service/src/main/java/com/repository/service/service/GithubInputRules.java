package com.repository.service.service;

import java.util.regex.Pattern;

// Checks for values that go into GitHub's URLs.
public final class GithubInputRules {

    private static final Pattern GITHUB_USERNAME = Pattern.compile("[A-Za-z0-9-]{1,39}");
    private static final Pattern REPOSITORY_NAME = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9._-]{0,99}");
    private static final Pattern BlOB_SHA = Pattern.compile("[0-9a-f]{40}");

    private GithubInputRules() {
    }

    public static void requireOwner(String githubOwner) {
        if (githubOwner == null || !GITHUB_USERNAME.matcher(githubOwner).matches()) {
            throw new IllegalArgumentException("githubOwner must be a valid GitHub username");
        }
    }

    public static void requireRepositoryName(String repoName) {
        if (repoName == null || !REPOSITORY_NAME.matcher(repoName).matches()) {
            throw new IllegalArgumentException("repositoryName is not valid GitHub repository name");
        }
    }

    public static void requireBlobSha(String sha) {
        if (sha == null || !BlOB_SHA.matcher(sha).matches()) {
            throw new IllegalArgumentException("sha must be 40 lowercase hex characters");
        }
    }
}
