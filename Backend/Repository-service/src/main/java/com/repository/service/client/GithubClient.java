package com.repository.service.client;

import com.repository.service.exception.GithubApiException;
import com.repository.service.github.GithubRepository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

// Only this class talks to GitHub. Every GitHub URL and every GitHub error is handled here.
@Component
public class GithubClient {
    private final RestClient restClient;

    // Constructor
    public GithubClient(RestClient githubRestClient) {
        this.restClient = githubRestClient;
    }

    // Returns one page of public repositories owned by a GitHub user.
    public List<GithubRepository> listPublicRepositories(String githubOwner, int page, int perPage) {
        return call(() -> restClient.get()
                  .uri("/users/{githubOwner}/repos?type=public&sort=updated&page={page}&per_page={perPage}",
                       githubOwner, page, perPage)
                  .retrieve()
                  .body(new ParameterizedTypeReference<List<GithubRepository>>() {}),
                "GitHub user not found: " + githubOwner);
    }

    // Gets one repository's details.
    public GithubRepository getRepository(String githubOwner, String repoName) {
        return call(() -> restClient.get()
                  .uri("/repos/{githubOwner}/{repoName}", githubOwner, repoName)
                  .retrieve()
                  .body(GithubRepository.class),
                "Repository not found: " + githubOwner + "/" + repoName);
    }

    // Gets the latest commit on one branch. The commit SHA identifies the exact code version.
    public GithubCommit getBranchCommit(String githubOwner, String repoName, String branch) {
        return call(() -> restClient.get()
                  .uri("/repos/{githubOwner}/{repoName}/commits/{branch}", githubOwner, repoName, branch)
                  .retrieve()
                  .body(GithubCommit.class),
                "Branch not found: " + branch);
    }

    // Gets every path in one version of the repository, in one request.
    public GithubTree getTree(String githubOwner, String repoName, String treeSha) {
        return call(() -> restClient.get()
                  .uri("/repos/{githubOwner}/{repoName}/git/trees/{sha}?recursive=1", githubOwner, repoName, treeSha)
                  .retrieve()
                  .body(GithubTree.class),
                "Tree not found for " + githubOwner + "/" + repoName);
    }

    // Gets the raw content of the file, identified by its blob SHA.
    public GithubBlob getBlob(String githubOwner, String repoName, String blobSha) {
        return call(() -> restClient.get()
                  .uri("/repos/{githubOwner}/{repoName}/git/blobs/{sha}}", githubOwner, repoName, blobSha)
                  .retrieve()
                  .body(GithubBlob.class),
                "File not found for blob " + blobSha);
    }

    // Runs one GitHub request and turns any failure into an error our service understands.
    private <T> T call(Supplier<T> request, String notFoundMessage) {
        try {
            return request.get();
        }
        catch (HttpClientErrorException.NotFound ex) {
            // Returns 404 when the username doesn't exist
            throw new NoSuchElementException(notFoundMessage);
        }
        catch (HttpClientErrorException ex) {
            // 403 or 429 usually means the rate limit is used up
            int code = ex.getStatusCode().value();
            if (code == 403 || code == 429) {
                throw new GithubApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "GitHub rate limit reached. Add a GITHUB_TOKEN or wait and try again.");
            }
            throw new GithubApiException(HttpStatus. BAD_GATEWAY,
                    "GitHub rejected the request with status " + code);
        }
        catch (RestClientException ex) {
            // Network problems or GitHub server errors.
            throw new GithubApiException(HttpStatus.BAD_GATEWAY,
                    "Could not get a valid answer from GitHub: " + ex.getMessage());
        }
    }
}
