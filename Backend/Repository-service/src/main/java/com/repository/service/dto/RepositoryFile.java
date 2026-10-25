package com.repository.service.dto;

// One file in the repository. This is the shape the indexing service will read.
public record RepositoryFile(
        String path,
        String sha,
        long size
) {
}
