package com.indexing.service.controller;

import com.indexing.service.dto.IndexingStatusResponse;
import com.indexing.service.dto.StartIndexingRequest;
import com.indexing.service.entity.IndexedRepository;
import com.indexing.service.pipeline.IndexingRunnerService;
import com.indexing.service.service.IndexingJobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/indexing/repositories")
public class IndexingController {

    // The header the gateway sets after it checks the session.
    static final String USER_HEADER = "X-DevPilot-User";

    private final IndexingJobService indexingJobService;
    private final IndexingRunnerService indexingRunnerService;

    // Constructor
    public IndexingController(IndexingJobService indexingJobService, IndexingRunnerService indexingRunnerService) {
        this.indexingJobService = indexingJobService;
        this.indexingRunnerService = indexingRunnerService;
    }

// ------ POST -> /api/indexing/repositories ------
    // Marks the job RUNNING, starts the background work, and returns right away with 202.
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IndexingStatusResponse start(@RequestHeader(USER_HEADER) String userLogin,
                                        @Valid @RequestBody StartIndexingRequest request) {
        // Save the RUNNING status first. The background job then sees a committed row.
        IndexedRepository repo = indexingJobService.startIndexing(userLogin, request.githubOwner(), request.repoName());

        // Start the background work. This call returns immediately because of @Async
        indexingRunnerService.run(repo.getId(), request.githubOwner(), request.repoName());
        return IndexingStatusResponse.from(repo);
    }

// ------ GET -> /api/indexing/repositories/{githubOwner}/{repoName} ------
    // Returns the current status
    @GetMapping("/{githubOwner}/{repoName}")
    public IndexingStatusResponse status(@RequestHeader(USER_HEADER) String userLogin,
                                         @PathVariable("githubOwner") String githubOwner,
                                         @PathVariable("repoName") String repoName) {
        IndexedRepository repo = indexingJobService.getStatus(userLogin, githubOwner, repoName);
        return IndexingStatusResponse.from(repo);
    }
}
