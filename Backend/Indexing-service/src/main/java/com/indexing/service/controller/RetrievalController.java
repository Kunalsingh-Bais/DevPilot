package com.indexing.service.controller;

import com.indexing.service.retrieval.ChunkRetrieverService;
import com.indexing.service.retrieval.RetrievedChunk;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Exposes the retriever over HTTP, so other services (such as the chat service) can search a repository.
@RestController
@RequestMapping("/api/indexing/repositories")
public class RetrievalController {

    private final ChunkRetrieverService chunkRetrieverService;

    // Constructor injection
    public RetrievalController(ChunkRetrieverService chunkRetrieverService) {
        this.chunkRetrieverService = chunkRetrieverService;
    }

// ------ GET -> /api/indexing/repositories/{owner}/{name}/search?question=...&limit=5 ------
    // Returns the chunks closest in meaning to the question, closest first.
    @GetMapping("/{githubOwner}/{repoName}/search")
    public List<RetrievedChunk> search(
            @PathVariable("githubOwner") String githubOwner,
            @PathVariable("repoName") String repoName,
            @RequestParam("question") String question,
            @RequestParam(name = "limit", defaultValue = "5") int limit) {

        // The rules (empty question, limit range) live in ChunkRetriever, not here.
        return chunkRetrieverService.search(githubOwner, repoName, question, limit);
    }
}
