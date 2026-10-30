package com.indexing.service.pipeline;

import java.util.List;

// The contract for saving chunks
public interface ChunkStore {

    void replaceChunks(long repositoryId, String commitSha, List<EmbeddedChunk> chunks);

}
