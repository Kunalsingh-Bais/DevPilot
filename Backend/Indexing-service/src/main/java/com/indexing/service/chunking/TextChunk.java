package com.indexing.service.chunking;

// Chunk :- One piece of file. It contains 80 lines of file.
public record TextChunk(
        int startLine,
        int endLine,
        String content
) {

    // Constructor
    // It rejects impossible line ranges
    public TextChunk {
        if (startLine < 1 || endLine < startLine) {
            throw new IllegalArgumentException("Invalid line range: " + startLine + " to " + endLine);
        }
    }
}
