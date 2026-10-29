package com.indexing.service.repoClient;

// DTO: One file in the list from repo-service.
public record RepoFileEntry(
        String path,
        String sha,
        long size
) {
}
