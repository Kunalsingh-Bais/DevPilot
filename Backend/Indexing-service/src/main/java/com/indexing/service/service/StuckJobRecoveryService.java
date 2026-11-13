package com.indexing.service.service;

import com.indexing.service.entity.IndexedRepository;
import com.indexing.service.entity.IndexingStatus;
import com.indexing.service.repository.IndexedRepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Runs once when the app has started, and fixes jobs that were interrupted by a previous shutdown
@Component
public class StuckJobRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(StuckJobRecoveryService.class);

    // Shown to the user, so they know why the job failed
    static final String MESSAGE = "Indexing was interrupted because the service stopped. Start the job again.";

    private final IndexedRepositoryRepository repositoryRepository;

    // Constructor injection
    public StuckJobRecoveryService(IndexedRepositoryRepository repositoryRepository) {
        this.repositoryRepository = repositoryRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void failStuckJobs() {
        List<IndexedRepository> stuck = repositoryRepository.findByStatus(IndexingStatus.RUNNING);

        for (IndexedRepository repo : stuck) {
            repo.setStatus(IndexingStatus.FAILED);
            repo.setErrorMessage(MESSAGE);
        }

        repositoryRepository.saveAll(stuck);

        if (!stuck.isEmpty()) {
            log.warn("Marked {} interrupted indexing job(s) as FAILED at startup.", stuck.size());
        }
    }
}
