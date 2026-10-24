package com.repository.service.service;

import com.repository.service.client.GithubClient;
import com.repository.service.dto.RepositorySummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class RepositoryService {

    private static final int MAX_PER_PAGE = 100;
    private static final Pattern GITHUB_USERNAME = Pattern.compile("[A-Za-z0-9-]{1,39}");

    @Autowired
    private GithubClient githubClient;

// ------ Lists one page of public repositories for a GitHub user ------
    public List<RepositorySummary> listRepositories(String githubOwner, int page, int perPage) {
        // Check input before calling GitHub
        if (githubOwner == null || !GITHUB_USERNAME.matcher(githubOwner).matches()) {
            throw new IllegalArgumentException("githubOwner must be a valid GitHub username");
        }
        if (page < 1) {
            throw new IllegalArgumentException("page must be 1 or greater");
        }
        if (perPage < 1 || perPage > MAX_PER_PAGE) {
            throw new IllegalArgumentException("perPage must be between 1 and " + MAX_PER_PAGE);
        }

        // Ask GitHub for the new data, then convert each item to our repsonse shape
        return githubClient.listPublicRepositories(githubOwner, page, perPage).stream()
                .map(RepositorySummary::from)
                .toList();
    }


}
