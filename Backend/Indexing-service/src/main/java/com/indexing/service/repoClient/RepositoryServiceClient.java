package com.indexing.service.repoClient;

import com.indexing.service.exception.RepositoryServiceException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

// Calls repository-service
@Component
public class RepositoryServiceClient {

    private final RestClient restClient;

    // Constructor injection
    public RepositoryServiceClient(RestClient repositoryServiceRestClient) {
        this.restClient = repositoryServiceRestClient;
    }

// ------ Asks repository-service for the file list of a repository's default branch ------
    public RepoFileListing listFiles(String githubOwner, String repoName) {
        return call(() -> restClient.get()
                        .uri("/api/repos/{githubOwner}/{repoName}/files", githubOwner, repoName)
                        .retrieve()
                        .body(RepoFileListing.class),
                    "Repository not found: " + githubOwner + "/" + repoName);
    }

// ------ Asks repository-service for one file's text. ------
    public Optional<RepoFileContent> getFileContent(String githubOwner, String repoName, String sha) {
        try {
            return Optional.ofNullable(call(() -> restClient.get()
                        .uri("/api/repos/{githubOwner}/{repoName}/blobs/{sha}", githubOwner, repoName, sha)
                        .retrieve()
                        .body(RepoFileContent.class),
                    "File not found: " + sha));
        }
        catch (RepositoryServiceException ex) {
            // 422 is a normal "skip this file" answer
            if (ex.getStatus() == HttpStatus.UNPROCESSABLE_CONTENT) {
                return Optional.empty();
            }
            throw ex;
        }
    }

// ------ Runs one call to repository-service and turns every failure into an exception we understand. ------
    private <T> T call(Supplier<T> request, String notFoundMessage) {
        try {
            return request.get();
        }
        catch (RestClientResponseException ex) {
            // repository-service answered, but with an error status.
            int code = ex.getStatusCode().value();

            if (code == 404) {
                throw new NoSuchElementException(notFoundMessage);
            }

            if (code >= 500) {
                // repository-service itself failed
                throw new RepositoryServiceException(HttpStatus.BAD_GATEWAY,
                        "repository-service failed with status " + code);
            }

            // Other 4xx errors keep their status, so the 422 check above can see it.
            HttpStatus status = HttpStatus.resolve(code);

            throw new RepositoryServiceException(
                    status != null ? status : HttpStatus.BAD_GATEWAY,
                    "repository-service rejected the request: " + ex.getResponseBodyAsString());
        }
        catch (ResourceAccessException ex) {
            // No answer at all: repository-service is stopped, the port is wrong, or the call timed out.
            throw new RepositoryServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "repository-service is not reachable. Check that it is running on the configured  port.");
        }
        catch (RestClientException ex) {
            // Any other HTTP-layer problem
            throw new RepositoryServiceException(HttpStatus.BAD_GATEWAY,
                    "Could not read the answer from repository-service: " + ex.getMessage());
        }
    }
}
