package com.indexing.service.dto;

import com.indexing.service.entity.IndexedRepository;
import com.indexing.service.entity.IndexingStatus;

import java.time.Instant;

// JSON we send back to client
public record IndexingStatusResponse (
        Long id,
        String githubOwner,
        String repoName,
        IndexingStatus status,
        String commitSha,
        String errorMessage,
        Instant createdAt
) {

    // Copies the values from the entity into this response shape
    public static IndexingStatusResponse from(IndexedRepository repo) {
        return new IndexingStatusResponse(
                repo.getId(),
                repo.getGithubOwner(),
                repo.getRepoName(),
                repo.getStatus(),
                repo.getCommitSha(),
                repo.getErrorMessage(),
                repo.getCreatedAt()
        );
    }
}
