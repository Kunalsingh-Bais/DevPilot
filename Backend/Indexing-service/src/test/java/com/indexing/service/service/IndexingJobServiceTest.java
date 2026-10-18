package com.indexing.service.service;

import com.indexing.service.entity.IndexedRepository;
import com.indexing.service.entity.IndexingStatus;
import com.indexing.service.repository.IndexedRepositoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IndexingJobServiceTest {

    // A fake repository
    @Mock
    private IndexedRepositoryRepository repositoryRepository;

    // Real service
    @InjectMocks
    private IndexingJobService service;

    @Test
    void startIndexingCreatesNewRepositoryAndMarksItRunning() {
        // No row exists yet, and save returns whatever it receives
        when(repositoryRepository.findByGithubOwnerAndRepoName("spring-projects", "spring-boot"))
                .thenReturn(Optional.empty());
        when(repositoryRepository.save(any(IndexedRepository.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        IndexedRepository result = service.startIndexing("spring-projects","spring-boot");

        // Assert
        assertThat(result.getStatus()).isEqualTo(IndexingStatus.RUNNING);
    }

    @Test
    void startIndexingFailsWhenAlreadyRunning() {
        // repository that is already running
        IndexedRepository running = new IndexedRepository("spring-projects", "spring-boot");
        running.setStatus(IndexingStatus.RUNNING);

        when(repositoryRepository.findByGithubOwnerAndRepoName("spring-projects", "spring-boot"))
                .thenReturn(Optional.of(running));

        // Act and Assert: starting again must be rejected
        assertThatThrownBy(() -> service.startIndexing("spring-projects", "spring-boot"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void markCompletedStoresCommitAndMarksCompleted() {
        IndexedRepository running = new IndexedRepository("spring-projects", "spring-boot");
        running.setStatus(IndexingStatus.RUNNING);

        when(repositoryRepository.findById(1L)).thenReturn(Optional.of(running));
        when(repositoryRepository.save(any(IndexedRepository.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        IndexedRepository result = service.markCompleted(1L, "abc123");

        // Assert
        assertThat(result.getStatus()).isEqualTo(IndexingStatus.COMPLETED);
        assertThat(result.getCommitSha()).isEqualTo("abc123");
    }

    @Test
    void markCompletedFailsWhenJobIsNotRunning() {
        // A repository that is still PENDING (never started)
        IndexedRepository pending = new IndexedRepository("spring-projects", "spring-boot");
        when(repositoryRepository.findById(1L)).thenReturn(Optional.of(pending));

        // Act and Assert:  a job that never started can't be completed.
        assertThatThrownBy(() -> service.markCompleted(1L, "abc123"))
                .isInstanceOf(IllegalStateException.class);
    }
}
