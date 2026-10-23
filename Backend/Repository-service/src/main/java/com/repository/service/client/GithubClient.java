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

// Only this class talks to GitHub
@Component
public class GithubClient {
    private final RestClient restClient;

    // Constructor
    public GithubClient(RestClient githubRestClient) {
        this.restClient = githubRestClient;
    }

    // Returns one page of public repositories owned by a GitHub user.
    public List<GithubRepository> listPublicRepositories(String githubOwner, int page, int perPage) {
        try {
            return restClient.get()
                    .uri("/user/{githubOwner}/repos?type=public&sort=updated&page={page}&per_page={perPage}",
                            githubOwner, page, perPage)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GithubRepository>>() {});
        }
        catch (HttpClientErrorException.NotFound ex) {
            // Returns 404 when the username doesn't exist
            throw new NoSuchElementException("GitHub user not found: " + githubOwner);
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
