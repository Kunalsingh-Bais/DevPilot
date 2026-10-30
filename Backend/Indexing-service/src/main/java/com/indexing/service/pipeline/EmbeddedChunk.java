package com.indexing.service.pipeline;

// A chunk plus its vector.It gets saved to the database.
public record EmbeddedChunk(
        SourceChunk source,        // where the text came from
        float[] embedding          // 768 numbers that represent the meaning of the text
) {
}
