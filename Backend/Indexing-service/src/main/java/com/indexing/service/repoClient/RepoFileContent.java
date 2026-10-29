package com.indexing.service.repoClient;

// DTO: The JSON repo-service returns for GET /api/repos/{githubOwner}/{repoName}/blobs/{sha}.
public record RepoFileContent(
        String sha,
        long size,
        String content        // the decoded text of the file
) {
}
