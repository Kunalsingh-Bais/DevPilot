package com.indexing.service.repoClient;

import java.util.List;

// DTO: The JSON repository-service returns for GET /api/repos/{githubOwner}/{repoName}/files.
public record RepoFileListing(
        String githubOwner,
        String repoName,
        String defaultBranch,         // the branch that was listed
        String commitSha,             // the exact commit the list came from
        List<RepoFileEntry> files
) {
}
