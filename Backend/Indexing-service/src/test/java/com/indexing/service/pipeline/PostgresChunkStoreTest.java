package com.indexing.service.pipeline;

import com.indexing.service.entity.IndexedRepository;
import com.indexing.service.repository.IndexedRepositoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Runs against the real PostgreSQL + pgvector database from Docker.
// Each test creates its own temporary repository, so it never touches real data.
@SpringBootTest
class PostgresChunkStoreTest {

    private static final int DIMENSIONS = 768;
    private static final String COMMIT_1 = "a".repeat(40);
    private static final String COMMIT_2 = "b".repeat(40);

    @Autowired
    private ChunkStore chunkStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IndexedRepositoryRepository indexedRepositoryRepository;

    private IndexedRepository testRepo;

    // Creates a fresh repository row before each test. Chunks need a repository to belong to
    @BeforeEach
    void createTestRepository() {
        String name = "store-test-" + UUID.randomUUID().toString().substring(0, 8);
        testRepo = indexedRepositoryRepository.save(new IndexedRepository("devpilot-test", name));
    }

    // Deleting the repository also deletes its chunks, because the V1 migration uses ON DELETE CASCADE
    @AfterEach
    void deleteTestRepository() {
        if (testRepo != null) {
            indexedRepositoryRepository.deleteById(testRepo.getId());
        }
    }

    @Test
    void storesChunksWithVectorsOfTheRightSize() {
        chunkStore.replaceChunks(testRepo.getId(), COMMIT_1, List.of(
                chunk("src/App.java", 1, 40),
                chunk("src/User.java", 1, 25)
        ));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM code_chunks WHERE repository_id = ?", Integer.class, testRepo.getId());
        Integer dimensions = jdbcTemplate.queryForObject(
                "SELECT vector_dims(embedding) FROM code_chunks WHERE repository_id = ? LIMIT 1",
                Integer.class, testRepo.getId());

        assertThat(count).isEqualTo(2);
        assertThat(dimensions).isEqualTo(DIMENSIONS);
    }

    @Test
    void replacingChunksRemovesTheOldOnes() {
        chunkStore.replaceChunks(testRepo.getId(), COMMIT_1, List.of(
                chunk("old1.java", 1, 10),
                chunk("old2.java", 1, 10),
                chunk("old3.java", 1, 10)
        ));

        // Reindex with a new commit and only one chunk
        chunkStore.replaceChunks(testRepo.getId(), COMMIT_2, List.of(chunk("new.java", 1, 5)));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM code_chunks WHERE repository_id = ?", Integer.class, testRepo.getId()
        );
        String commit = jdbcTemplate.queryForObject(
                "SELECT commit_sha FROM code_chunks WHERE repository_id = ?", String.class, testRepo.getId()
        );

        // Only the new chunk remains, and it comes from the new commit
        assertThat(count).isEqualTo(1);
        assertThat(commit).isEqualTo(COMMIT_2);
    }

    @Test
    void failedInsertKeepsTheOldChunks() {
        chunkStore.replaceChunks(testRepo.getId(), COMMIT_1, List.of(
                chunk("kept.java", 1, 10)));

        // The second chunk has only 10 numbers, so PostgreSQL rejects the whole batch.
        List<EmbeddedChunk> broken = List.of(
                chunk("new.java", 1, 5),
                new EmbeddedChunk(new SourceChunk("bad.java", 1, 1,"x"), new float[10])
        );

        assertThatThrownBy(() -> chunkStore.replaceChunks(testRepo.getId(), COMMIT_2, broken))
                .isInstanceOf(DataAccessException.class);

        // delete was rolled back, so the old chunk is still there
        String filePath = jdbcTemplate.queryForObject(
                "SELECT file_path FROM code_chunks WHERE repository_id = ?", String.class, testRepo.getId()
        );

        assertThat(filePath).isEqualTo("kept.java");
    }

    // Builds a chunk with a valid 768-number vector. Every number is 0.1
    private static EmbeddedChunk chunk(String filePath, int startLine, int endLine) {
        float[] vector = new float[DIMENSIONS];
        Arrays.fill(vector, 0.1f);
        return new EmbeddedChunk(
                new SourceChunk(filePath, startLine, endLine, "code in " + filePath), vector
        );
    }

}
