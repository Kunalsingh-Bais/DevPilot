package com.indexing.service.embedding;

import java.util.List;

// This turns text into vectors
public interface EmbeddingProvider {

    // Returns one vector per input text, in the same order.
    // Each vector must have 768 numbers to match the vector(768) column in the database.
    List<float[]> embed(List<String> texts);
}
