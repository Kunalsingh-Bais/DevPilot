package com.indexing.service.pipeline;

import org.springframework.stereotype.Component;

import java.util.List;

// Temporary placeholder until the pgvector storage is written.
// TODO: DELETE this file when the real store is added.
@Component
public class UnconfiguredChunkStore implements ChunkStore {

    @Override
    public void replaceChunks(long repositoryId, String commitSha, List<EmbeddedChunk> chunks) {
        throw new IllegalStateException("Chunk storage is not configured yet. It will be added with the pgvector step.");
    }
}
