package com.indexing.service.embedding;

import org.springframework.stereotype.Component;

import java.util.List;

// Temporary placeholder. It lets the app start, and it fails any indexing job with a clear reason.
// TODO: // DELETE this file when the Ollama provider is added
@Component
public class UnconfiguredEmbeddingProvider implements EmbeddingProvider {

    public List<float[]> embed(List<String> texts) {
        throw new IllegalStateException("Embedding model is not configured yet. Ollama will be connected in the next step.");
    }
}
