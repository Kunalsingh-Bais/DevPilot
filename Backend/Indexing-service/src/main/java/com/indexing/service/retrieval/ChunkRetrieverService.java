package com.indexing.service.retrieval;

import com.indexing.service.embedding.EmbeddingProvider;
import com.indexing.service.embedding.VectorText;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

// Finds the stored chunks that are closest in meaning to a question, inside one repository.
@Service
public class ChunkRetrieverService {

    // Limits that protect the embedding model and the database from oversized requests.
    static final int MAX_QUESTION_CHARS = 1000;
    static final int MAX_LIMIT = 20;     // maximum chunks the caller can request

    // query to search
    private static final String SEARCH_SQL = """
            SELECT c.id, c.file_path, c.start_line, c.end_line, c.content,
                   c.embedding <=> ?::vector AS distance
            FROM code_chunks c
            JOIN repositories r ON r.id = c.repository_id
            WHERE r.github_owner = ?
              AND r.repo_name = ?
            ORDER BY c.embedding <=> ?::vector
            LIMIT ?
            """;

    private final EmbeddingProvider embeddingProvider;
    private final JdbcTemplate jdbcTemplate;

    // Constructor injection
    public ChunkRetrieverService(EmbeddingProvider embeddingProvider, JdbcTemplate jdbcTemplate) {
        this.embeddingProvider = embeddingProvider;
        this.jdbcTemplate = jdbcTemplate;
    }

// ------ Returns up to "limit" chunks from one repository, closest to the question first ------
    public List<RetrievedChunk> search(String githubOwner, String repoName, String question, int limit) {
        // Check the input before calling the embedding model, which costs time.
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be empty");
        }

        if (question.length() > MAX_QUESTION_CHARS) {
            throw new IllegalArgumentException("question must be " + MAX_QUESTION_CHARS + " characters or fewer");
        }

        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }

        // Turn the question into a vector, using the same model that indexed the code.
        float[] queryVector = embeddingProvider.embed(List.of(question.trim())).get(0);
        String vectorText = VectorText.toLiteral(queryVector);

        // The "?" marks are filled in order: vector, owner, name, vector again, limit.
        return jdbcTemplate.query(
                SEARCH_SQL,
                (rs, rowNum) -> new RetrievedChunk(
                        rs.getLong("id"),
                        rs.getString("file_path"),
                        rs.getInt("start_line"),
                        rs.getInt("end_line"),
                        rs.getString("content"),
                        rs.getDouble("distance")),
                vectorText, githubOwner, repoName, vectorText, limit
        );
    }
}
