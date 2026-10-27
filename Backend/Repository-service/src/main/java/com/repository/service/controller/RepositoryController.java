package com.repository.service.controller;

import com.repository.service.dto.RepositoryFileContent;
import com.repository.service.dto.RepositoryFileResponse;
import com.repository.service.dto.RepositorySummary;
import com.repository.service.service.FileContentService;
import com.repository.service.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repos")
public class RepositoryController {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private FileContentService fileContentService;

// ------ GET -> /api/repos/{githubOwner}?page=1&perPage=30 ------
    // Lists one page of public repositories for a user.
    @GetMapping("/{githubOwner}")
    public List<RepositorySummary> listRepositories(
            @PathVariable("githubOwner") String githubOwner,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "perPage", defaultValue = "30") int perPage) {

        return repositoryService.listRepositories(githubOwner, page, perPage);
    }

// ------ GET -> /api/repos/{githubOwner}/{repoName}/files ------
    // Lists every file in the latest version of the default branch.
    @GetMapping("/{githubOwner}/{repoName}/files")
    public RepositoryFileResponse listFiles(
            @PathVariable("githubOwner") String githubOwner,
            @PathVariable("repoName") String repoName) {

        return repositoryService.listFiles(githubOwner, repoName);
    }

// ------ GET -> /api/repos/{githubOwner}/{repoName}/blobs/{sha} ------
    // Returns the decode text of one file
    @GetMapping("/{githubOwner}/{repoName}/blobs/{sha}")
    public RepositoryFileContent getFileContent(
            @PathVariable("githubOwner") String githubOwner,
            @PathVariable("repoName") String repoName,
            @PathVariable("sha") String sha)   {

        return fileContentService.getFileContent(githubOwner, repoName, sha);
    }
}
