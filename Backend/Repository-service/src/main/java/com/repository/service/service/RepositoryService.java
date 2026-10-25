package com.repository.service.service;

import com.repository.service.client.GithubClient;
import com.repository.service.client.GithubCommit;
import com.repository.service.client.GithubTree;
import com.repository.service.dto.RepositoryFile;
import com.repository.service.dto.RepositoryFileResponse;
import com.repository.service.dto.RepositorySummary;
import com.repository.service.exception.GithubApiException;
import com.repository.service.github.GithubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class RepositoryService {

    private static final int MAX_PER_PAGE = 100;
    private static final Pattern GITHUB_USERNAME = Pattern.compile("[A-Za-z0-9-]{1,39}");

    private static final Pattern REPOSITORY_NAME = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9._-]{0,99}");

    @Autowired
    private GithubClient githubClient;

// ------ Method 1: Lists one page of public repositories for a GitHub user ------
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

// ------ Method 2: Lists every file in the latest version of the default branch ------
    public RepositoryFileResponse listFiles(String githubOwner, String repoName) {
        // Validate GitHub owner name and repo name
        if (githubOwner == null || !GITHUB_USERNAME.matcher(githubOwner).matches()) {
            throw new IllegalArgumentException("githubOwner must be a valid GitHub username");
        }

        if (repoName == null || !REPOSITORY_NAME.matcher(repoName).matches()) {
            throw new IllegalArgumentException("repoName is not a valid GitHub repository name");
        }

        // Step 1: find the default branch
        GithubRepository repo = githubClient.getRepository(githubOwner, repoName);
        String branch = repo.defaultBranch();

        // check branch is empty
        if (branch == null || branch.isBlank()) {
            throw new GithubApiException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Repository has no default branch yet. It may be empty.");
        }

        // Step 2: find the latest commit on that branch
        GithubCommit commit = githubClient.getBranchCommit(githubOwner, repoName, branch);

        // Step 3: get every path in that commit's tree
        GithubTree tree = githubClient.getTree(githubOwner, repoName, commit.sha());

        if (tree.truncated()) {
            throw new GithubApiException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Repository is too large to list in one request.");
        }

        // Keep only files ("blob"). Folders ("tree") and submodules ("commit") are skipped.
        List<RepositoryFile> files = tree.tree().stream()
                .filter(entry -> "blob".equals(entry.type()))
                .map(entry -> new RepositoryFile(
                        entry.path(),
                        entry.sha(),
                        entry.size() == null ? 0L : entry.size()
                )).toList();

        return new RepositoryFileResponse(githubOwner, repoName, branch, commit.sha(), files);
    }
}
