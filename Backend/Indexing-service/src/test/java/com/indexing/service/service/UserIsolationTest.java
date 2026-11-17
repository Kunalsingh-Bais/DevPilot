package com.indexing.service.service;

import com.indexing.service.repository.IndexedRepositoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class UserIsolationTest {

    private static final String GITHUB_OWNER = "isolation-test-owner";
    private static final String REPO_NAME = "isolation-test-repo";

    @Autowired
    private IndexingJobService indexingJobService;

    @Autowired
    private IndexedRepositoryRepository indexedRepositoryRepository;

    @Autowired
    private JdbcTemplate jdbc;

    // Deletes the test rows, so the database stays clean for the real data.
    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM repositories WHERE github_owner = ?", GITHUB_OWNER);
    }

    @Test
    void sameRepositoryIsSeparatePerUser() {
        // Alice indexes the repository. Bob has no copy of it.
        indexingJobService.startIndexing("alice", GITHUB_OWNER, REPO_NAME);

        assertThat(indexedRepositoryRepository.findByUserLoginAndGithubOwnerAndRepoName("alice", GITHUB_OWNER, REPO_NAME)).isPresent();
        assertThat(indexedRepositoryRepository.findByUserLoginAndGithubOwnerAndRepoName("bob", GITHUB_OWNER, REPO_NAME)).isEmpty();
    }

    @Test
    void userCannotReadAnotherUsersStatus() {

        indexingJobService.startIndexing("alice", GITHUB_OWNER, REPO_NAME);

        // Bob asks for Alice's copy. It must look as if the repository does not exist.
        assertThatThrownBy(() -> indexingJobService.getStatus("bob", GITHUB_OWNER, REPO_NAME))
                .isInstanceOf(NoSuchElementException.class);
    }
}
