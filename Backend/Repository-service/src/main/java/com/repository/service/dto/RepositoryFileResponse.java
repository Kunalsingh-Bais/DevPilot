package com.repository.service.dto;

import java.util.List;

// The JSON we return for GET /api/repos/{githubOwner}/{repoName}/files.
public record RepositoryFileResponse(
        String githubOwner,
        String repoName,
        String defaultBranch,
        String commitSha,
        List<RepositoryFile> files
) {
}
