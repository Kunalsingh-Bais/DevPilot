package com.indexing.service.repository;

import com.indexing.service.entity.CodeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeChunkRepository extends JpaRepository<CodeChunk, Long> {

    // Returns all chunks that belong to one repository.
    List<CodeChunk> findByRepositoryId(Long repository);

    // Counts chunks for one repository. Useful for status and progress pages.
    Long countByRepositoryId(Long repositoryId);

    // Deletes all chunks for one repository.
    void deleteByRepositoryId(Long repositoryId);


}
