package com.indexing.service.pipeline;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Saves chunks and their vectors to the code_chunks table.
@Repository
public class PostgresChunkStore implements ChunkStore{

    // How many rows to send to PostgreSQL in one batch.
    private static final int BATCH_SIZE = 100;

    // Removes every chunk that belongs to one repository.
    private static final String DELETE_SQL = "DELETE FROM code_chunks WHERE repository_id = ?";

    // "?::vector" tells PostgreSQL to read the text in the parameter as a vector.
    private static final String INSERT_SQL = """
            INSERT into code_chunks(repository_id, file_path, start_line, end_line, content, commit_sha, embedding)
            values(?, ?, ?, ?, ?, ?, ?::vector)
            """;

    // Spring's helper for running SQL
    private final JdbcTemplate jdbcTemplate;

    public PostgresChunkStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

// ------ Deletes the old chunks and inserts the new ones, all in one transaction ------
    // If any insert fails, delete is rolled back, and the old chunks stay in place.
    @Override
    @Transactional
    public void replaceChunks(long repository_id, String commitSha, List<EmbeddedChunk> chunks) {
        // Step 1: remove the old chunks for this repository.
        jdbcTemplate.update(DELETE_SQL, repository_id);

        if (chunks.isEmpty()) {
            return;
        }

        // Step 2: insert the new chunks in batches
        jdbcTemplate.batchUpdate(INSERT_SQL, chunks, BATCH_SIZE, (ps, chunk) -> {
            var source = chunk.source();
            ps.setLong(1, repository_id);
            ps.setString(2, source.filePath());
            ps.setInt(3, source.startLine());
            ps.setInt(4, source.endLine());
            ps.setString(5, source.content());
            ps.setString(6, commitSha);
            ps.setString(7, toVectorLiteral(chunk.embedding()));
        });
    }

// ------ Turns a float array into pgvector's text format, for example "[0.1,0.2,0.3]" ------
    static String toVectorLiteral(float[] values) {
        StringBuilder text = new StringBuilder("[");
        for (int i=0; i < values.length; i++) {

            // NaN and infinity are not valid numbers in a vector, so refuse them early.
            if (!Float.isFinite(values[i])) {
                throw new IllegalArgumentException("Embedding contains a value that is not a finite number.");
            }
            if (i > 0) {
                text.append(',');
            }
            text.append(values[i]);
        }
        return text.append(']').toString();
    }
}
