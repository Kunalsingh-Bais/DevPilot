package com.indexing.service.service;

import com.indexing.service.entity.IndexedRepository;
import com.indexing.service.entity.IndexingStatus;
import com.indexing.service.repository.IndexedRepositoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IndexingJobService {

    private final IndexedRepositoryRepository indexedRepositoryRepository;

    // Constructor
    public IndexingJobService(IndexedRepositoryRepository indexedRepositoryRepository) {
        this.indexedRepositoryRepository = indexedRepositoryRepository;
    }

    // Starts indexing for a GitHub repository
    @Transactional
    public IndexedRepository startIndexing(String githubOwner, String repoName) {
        // Find the existing repository, or create a new one (which starts as PENDING)
        IndexedRepository repo = indexedRepositoryRepository.findByGithubOwnerAndRepoName(githubOwner, repoName)
                .orElseGet(() -> new IndexedRepository(githubOwner, repoName));

        // Check whether repository already being indexed (or already running)
        if (repo.getStatus() == IndexingStatus.RUNNING) {
            throw new IllegalStateException("Indexing already running for " + githubOwner + "/" + repoName);
        }

        // Start the job
        repo.setStatus(IndexingStatus.RUNNING);
        repo.setErrorMessage(null);
        return indexedRepositoryRepository.save(repo);
    }

    // Called when indexing finishes successfully
    @Transactional
    public IndexedRepository markCompleted(Long repositoryId, String commitSha) {
        IndexedRepository repo = findOrThrow(repositoryId);

        // Check current status
        requireStatus(repo, IndexingStatus.RUNNING);

        // Record which commit we indexed
        repo.setCommitSha(commitSha);
        repo.setStatus(IndexingStatus.COMPLETED);
        return indexedRepositoryRepository.save(repo);
    }

    // Called when indexing fails.
    @Transactional
    public IndexedRepository markFailed(Long repositoryId, String errorMessage) {
        IndexedRepository repo = findOrThrow(repositoryId);

        // check status is running
        requireStatus(repo, IndexingStatus.RUNNING);

        repo.setStatus(IndexingStatus.FAILED);
        repo.setErrorMessage(errorMessage);
        return indexedRepositoryRepository.save(repo);
    }

// ------ Helper Methods ------

    // Load a repository by ID, or stop with a clear message
    private IndexedRepository findOrThrow(Long repositoryId) {
        return indexedRepositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new IllegalArgumentException("No repository with id " + repositoryId));
    }

    // Make sure the job is in the state we expect before changing it
    private void requireStatus(IndexedRepository repo, IndexingStatus expected) {
        if (repo.getStatus() != expected) {
            throw new IllegalStateException("Expected status " + expected + " but found " + repo.getStatus());
        }
    }
}
