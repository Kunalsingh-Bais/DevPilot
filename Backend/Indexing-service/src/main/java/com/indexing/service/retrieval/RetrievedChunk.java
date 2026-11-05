package com.indexing.service.retrieval;

// One chunk returned by a search, with its location and how close it is to the question.
public record RetrievedChunk(
        long id,
        String filePath,
        int startLine,
        int endLine,
        String content,
        double distance         // cosine distance
) {
}
