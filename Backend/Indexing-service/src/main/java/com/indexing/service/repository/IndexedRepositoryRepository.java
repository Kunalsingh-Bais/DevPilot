package com.indexing.service.repository;

import com.indexing.service.entity.IndexedRepository;
import com.indexing.service.entity.IndexingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndexedRepositoryRepository extends JpaRepository<IndexedRepository, Long> {

    // Finds one repository by its GitHub owner and name
    Optional<IndexedRepository> findByGithubOwnerAndRepoName(String githubOwner, String repoName);

    // Returns true if this owner/name pair is already in the table
    boolean existsByGithubOwnerAndRepoName(String githubOwner, String repoName);

    // Finds one user's copy of a GitHub repository. Other users' copies are never returned.
    Optional<IndexedRepository> findByUserLoginAndGithubOwnerAndRepoName(String userLogin, String githubOwner, String repoName);

    // Returns every repository with the given status. Used at startup to find jobs left RUNNING
    List<IndexedRepository> findByStatus(IndexingStatus status);

    // Returns every repository this user has indexed, newest first.
    List<IndexedRepository> findByUserLoginOrderByCreatedAtDesc(String userLogin);
}
