package com.indexing.service.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    // Must match the vector(768) column in the database
    static final int EXPECTED_DIMENSIONS = 768;

    // Spring AI creates this bean from Ollama settings
    private final EmbeddingModel embeddingModel;

    // Constructor injection
    public OllamaEmbeddingProvider(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        // If text is empty, nothing to embed, so nothing to send to Ollama.
        if (texts.isEmpty()) {
            return List.of();
        }

        // Ask Spring AI to embed all texts.
        List<float[]> vectors = embeddingModel.embed(texts);

        // Check the vector size is 768 as defined in PostgreSQL database table.
        for (float[] vector : vectors) {
            if (vector.length != EXPECTED_DIMENSIONS) {
                throw new IllegalStateException("Embedding model returned " + vector.length
                        + " numbers, but the database expects " + EXPECTED_DIMENSIONS
                        + ". Check the model name in application.properties.");
            }
        }
        return vectors;
    }
}

