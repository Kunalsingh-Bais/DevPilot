package com.indexing.service.embedding;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Calls the real nomic-embed-text model through Ollama.
// If Ollama is not running, every test in this class is skipped instead of failing.
@SpringBootTest
class OllamaEmbeddingIT {

    @Autowired
    private EmbeddingProvider embeddingProvider;

    @BeforeAll
    static void requireOllama() {
        assumeTrue(ollamaIsRunning(), "Ollama is not running on localhost:11434, so the test is skipped.");
    }

    @Test
    void returnsOneVectorOf768NumbersPerText() {
        List<float[]> vectors = embeddingProvider.embed(List.of("How is user login handled?", "Hello"));

        assertThat(vectors).hasSize(2);
        assertThat(vectors.get(0)).hasSize(768);
    }

    @Test
    void sameTextGivesAlmostTheSameVector() {
        // Exact equality isn't required, because GPU math can differ in tiny ways.
        // A similarity close to 1.0 means the vectors point in the same direction.
        float[] first = embeddingProvider.embed(List.of("class UserService")).get(0);
        float[] second = embeddingProvider.embed(List.of("class UserService")).get(0);

        assertThat(cosineSimilarity(first, second)).isGreaterThan(0.999);
    }

    @Test
    void relatedMeaningScoresHigherThanUnrelated() {
        // Meaning check: the login question should sit closer to the login sentence
        // than to a sentence about cooking.
        List<float[]> vectors = embeddingProvider.embed(List.of(
                "How is user login handled?",
                "Authenticate the user with a password",
                "Banana bread recipe with walnuts"
        ));

        double related = cosineSimilarity(vectors.get(0), vectors.get(1));
        double unrelated = cosineSimilarity(vectors.get(0), vectors.get(2));

        assertThat(related).isGreaterThan(unrelated);
    }

    // Checks whether Ollama answers on its address. Returns false instead of throwing.
    private static boolean ollamaIsRunning() {
        try {
            HttpURLConnection connection = (HttpURLConnection)
                    URI.create("http://localhost:11434").toURL().openConnection();
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            return connection.getResponseCode() == 200;
        }
        catch (IOException ex) {
            return false;
        }
    }

    // Cosine similarity: 1.0 means the same direction, 0 means unrelated, and values near -1 mean opposite.
    private static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return dot/ (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
