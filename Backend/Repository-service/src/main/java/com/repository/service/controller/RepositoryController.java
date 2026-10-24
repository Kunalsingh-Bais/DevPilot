package com.repository.service.controller;

import com.repository.service.dto.RepositorySummary;
import com.repository.service.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/repos")
public class RepositoryController {

    @Autowired
    private RepositoryService repositoryService;

// ------ GET -> /api/repos/{githubOwner}?page=1&perPage=30 ------
    public List<RepositorySummary> listRepositories(
            @PathVariable("githubOwner") String githubOwner,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "perPage", defaultValue = "30") int perPage) {

        return repositoryService.listRepositories(githubOwner, page, perPage);
    }
}
