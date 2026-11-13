package com.indexing.service.service;

import com.indexing.service.entity.IndexedRepository;
import com.indexing.service.entity.IndexingStatus;
import com.indexing.service.repository.IndexedRepositoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StuckJobRecoveryServiceTest {

    @Mock
    private IndexedRepositoryRepository repositoryRepository;

    @InjectMocks
    private StuckJobRecoveryService recovery;

    @Test
    void marksInterruptedJobsAsFailedWithAReason() {
        // Arrange: one job that was left RUNNING when the service stopped.
        IndexedRepository stuck = new IndexedRepository("octocat", "Hello-World");
        stuck.setStatus(IndexingStatus.RUNNING);
        when(repositoryRepository.findByStatus(IndexingStatus.RUNNING)).thenReturn(List.of(stuck));

        recovery.failStuckJobs();

        // The job is now FAILED, with a message the user can read, and the change is saved.
        assertThat(stuck.getStatus()).isEqualTo(IndexingStatus.FAILED);
        assertThat(stuck.getErrorMessage()).isEqualTo(StuckJobRecoveryService.MESSAGE);
        verify(repositoryRepository).saveAll(anyList());
    }

    @Test
    void doesNothingWhenNoJobIsStuck() {
        when(repositoryRepository.findByStatus(IndexingStatus.RUNNING)).thenReturn(List.of());
        recovery.failStuckJobs();

        verify(repositoryRepository).saveAll(List.of());
    }
}
