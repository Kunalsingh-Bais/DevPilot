package com.indexing.service.repository;

import com.indexing.service.entity.IndexedRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IndexedRepositoryRepository extends JpaRepository<IndexedRepository, Long> {

    // Finds one repository by its GitHub owner and name
    Optional<IndexedRepository> findByGithubOwnerAndRepoName(String githubOwner, String repoName);

    // Returns true if this owner/name pair is already in the table
    boolean existsByGithubOwnerAndRepoName(String githubOwner, String repoName);
}
