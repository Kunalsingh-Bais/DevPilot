package com.indexing.service.pipeline;

// One chunk of text, before embedding. It remembers which file and which lines it came from.
public record SourceChunk(
        String filePath,
        int startLine,
        int endLine,
        String content
) {
}
