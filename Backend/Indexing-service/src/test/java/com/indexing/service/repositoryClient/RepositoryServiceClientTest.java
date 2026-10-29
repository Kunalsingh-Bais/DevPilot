package com.indexing.service.repositoryClient;

import com.indexing.service.exception.RepositoryServiceException;
import com.indexing.service.repoClient.RepoFileContent;
import com.indexing.service.repoClient.RepoFileListing;
import com.indexing.service.repoClient.RepositoryServiceClient;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RepositoryServiceClientTest {

    private static final String BASE = "http://localhost:8081";
    private static final String SHA = "a".repeat(40);

    private final RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final RepositoryServiceClient client = new RepositoryServiceClient(builder.build());

    @Test
    void listsFilesFromRepositoryService() {
        // Arrange: the fake repository-service returns one file in a JSON body.
        String json = """
                {
                  "githubOwner": "octocat",
                  "repoName": "Hello-World",
                  "defaultBranch": "main",
                  "commitSha": "%s",
                  "files": [
                    { "path": "README.md", "sha": "%s", "size": 12 }
                  ]
                }
                """.formatted(SHA, SHA);
        server.expect(requestTo(BASE + "/api/repos/octocat/Hello-World/files"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        // Act
        RepoFileListing listing = client.listFiles("octocat", "Hello-World");

        assertThat(listing.commitSha()).isEqualTo(SHA);
        assertThat(listing.files()).hasSize(1);
        assertThat(listing.files().get(0).path()).isEqualTo("README.md");
        server.verify();
    }

    @Test
    void skippedFileReturnsEmptyInsteadOfFailing() {
        // repository-service says 422: this file is binary or too large, so it can't be indexed.
        server.expect(requestTo(BASE + "/api/repos/octocat/Hello-World/blobs/" + SHA))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":422,\"detail\":\"File is binary\"}"));

        Optional<RepoFileContent> result = client.getFileContent("octocat", "Hello-World", SHA);

        assertThat(result).isEmpty();
    }

    @Test
    void unknownRepositoryThrowsNotFound() {
        server.expect(requestTo(BASE + "/api/repos/octocat/missing/files"))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.listFiles("octocat", "missing"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void serverErrorBecomesBadGateway() {
        // repository-service itself failed. Report it as a bad gateway with its own status in the message.
        server.expect(requestTo(BASE + "/api/repos/octocat/Hello-World/files"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.listFiles("octocat", "Hello-World"))
                .isInstanceOfSatisfying(RepositoryServiceException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}
