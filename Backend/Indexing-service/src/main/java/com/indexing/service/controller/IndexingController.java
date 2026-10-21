package com.indexing.service.controller;

import com.indexing.service.dto.IndexingStatusResponse;
import com.indexing.service.dto.StartIndexingRequest;
import com.indexing.service.entity.IndexedRepository;
import com.indexing.service.service.IndexingJobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/indexing/repositories")
public class IndexingController {

    private final IndexingJobService indexingJobService;

    // Constructor
    public IndexingController(IndexingJobService indexingJobService) {
        this.indexingJobService = indexingJobService;
    }

// ------ POST -> /api/indexing/repositories ------
    // Starts the indexing job
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IndexingStatusResponse start(@Valid @RequestBody StartIndexingRequest request) {
        IndexedRepository repo = indexingJobService.startIndexing(request.githubOwner(), request.repoName());
        return IndexingStatusResponse.from(repo);
    }

// ------ GET -> /api/indexing/repositories/{githubOwner}/{repoName} ------
    // Returns the current status
    @GetMapping("/{githubOwner}/{repoName}")
    public IndexingStatusResponse status(@PathVariable("githubOwner") String githubOwner, @PathVariable("repoName") String repoName) {
        IndexedRepository repo = indexingJobService.getStatus(githubOwner, repoName);
        return IndexingStatusResponse.from(repo);
    }
}
