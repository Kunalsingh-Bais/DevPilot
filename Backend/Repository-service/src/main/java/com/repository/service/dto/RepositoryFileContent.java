package com.repository.service.dto;

// The JSON we return for one file's content
public record RepositoryFileContent(
        String sha,           // blob SHA this content came from
        long size,            // size in bytes
        String content        // decoded text of the file
) {
}
