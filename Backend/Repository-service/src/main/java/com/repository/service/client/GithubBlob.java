package com.repository.service.client;

// The raw content of one file, encoded.
public record GithubBlob(
        String sha,
        long size,
        String content,      // the file's bytes as base64 text
        String encoding      // "base64" for file content
) {
}
