package com.chat.service.client;

import com.chat.service.exception.ChatException;
import org.apache.logging.log4j.util.Supplier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.NoSuchElementException;

// The only class in chat-service that calls indexing-service.
@Component
public class IndexingServiceClient {

    private final RestClient restClient;

    // Constructor injection: Spring passes in the client built in IndexingServiceConfig.
    public IndexingServiceClient(RestClient indexingServiceRestClient) {
        this.restClient = indexingServiceRestClient;
    }

// ------ Returns the indexing status of one repository, including the commit it was indexed from ------
    public RepositoryStatus getStatus(String githubOwner, String repoName) {
        return call(() -> restClient.get()
                      .uri("api/indexing/repositories/{githubOwner}/{repoName}", githubOwner, repoName)
                      .retrieve()
                      .body(RepositoryStatus.class),
                "Repository not found: " + githubOwner + "/" + repoName);
    }

// ------ Returns the chunks closest in meaning to the question, closest first ------
    public List<SearchHit> search(String githubOwner, String repoName, String question, int limit) {
        return call(() -> restClient.get()
                      .uri("api/indexing/repositories/{githubOwner}/{repoName}/search?question={question}&limit={limit}",
                              githubOwner, repoName, question, limit)
                      .retrieve()
                      .body(new ParameterizedTypeReference<List<SearchHit>>() {}),
                "Repository not found: " + githubOwner + "/" + repoName);
    }

// ------ Runs one call and turns every failure into an error we understand ------
    private <T> T call(Supplier<T> request, String notFoundMessage) {
        try {
            return request.get();
        }
        catch (RestClientResponseException ex) {
            // indexing-service answered with an error status.
            if (ex.getStatusCode().value() == 404) {
                throw new NoSuchElementException(notFoundMessage);
            }
            throw new ChatException(HttpStatus.BAD_GATEWAY,
                    "indexing-service answered with status " + ex.getStatusCode().value());
        }
        catch (ResourceAccessException ex) {
            // No answer at all: indexing-service is stopped, or the call timed out.
            throw new ChatException(HttpStatus.SERVICE_UNAVAILABLE,
                    "indexing-service is not reachable. Start it on port 8082.");
        }
        catch (RestClientException ex) {
            throw new ChatException(HttpStatus.BAD_GATEWAY,
                    "Could not read the answer from indexing-service.");
        }
    }

    // The fields need from GET /api/indexing/repositories/{owner}/{name}.
    public record RepositoryStatus(
            Long id,
            String githubOwner,
            String repoName,
            String status,
            String commitSha,
            String errorMessage
    ) {}

    // One chunk returned by the search endpoint.
    public record SearchHit(
            Long id,
            String filePath,
            int startLine,
            int endLine,
            String content,
            double distance
    ) {}
}
