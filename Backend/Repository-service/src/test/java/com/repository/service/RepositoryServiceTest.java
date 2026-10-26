package com.repository.service;

import com.repository.service.client.GithubClient;
import com.repository.service.client.GithubCommit;
import com.repository.service.client.GithubTree;
import com.repository.service.dto.RepositoryFile;
import com.repository.service.dto.RepositoryFileResponse;
import com.repository.service.dto.RepositorySummary;
import com.repository.service.exception.GithubApiException;
import com.repository.service.github.GithubRepository;
import com.repository.service.service.RepositoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RepositoryServiceTest {

    // A fake GitHub client, so the tests never call the real GitHub
    @Mock
    private GithubClient githubClient;

    // The real service, with the fake client passed in
    @InjectMocks
    private RepositoryService service;

    // Build fake GitHub repository with given default branch
    private GithubRepository fakeRepo(String defaultBranch) {
        return new GithubRepository("Hello-world","A test repo","Java",
                "public", defaultBranch, new GithubRepository.Owner("octocat"));
    }

    @Test
    void mapsGithubDataToOurSummary() {
        // Arrange: GitHub returns one repository
        GithubRepository fromGithub = new GithubRepository("spoon", "A Java tool", "Java", "public", "main",
                new GithubRepository.Owner("octocat"));
        when(githubClient.listPublicRepositories("octocat", 1, 30)).thenReturn(List.of(fromGithub));

        // Act
        List<RepositorySummary> result = service.listRepositories("octocat",1,30);

        // Assert: the githubOwner is flattened from the nested object into a plain string.
        assertThat(result).hasSize(1);
        assertThat(result.get(0).githubOwner()).isEqualTo("octocat");
        assertThat(result.get(0).repoName()).isEqualTo("spoon");
    }

    @Test
    void rejectsPageZeroWithoutCallingGithub() {
        assertThatThrownBy(() -> service.listRepositories("octocat", 0, 30))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(githubClient);
    }

    @Test
    void rejectsPerPageAboveOneHundred() {
        assertThatThrownBy(() -> service.listRepositories("octocat", 1, 101))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(githubClient);
    }

    @Test
    void rejectsOwnerWithSlash() {
        assertThatThrownBy(() -> service.listRepositories("octocat/../other", 1,30))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(githubClient);
    }

    @Test
    void listFilesKeepOnlyFilesAndSkipsFolders() {
        when(githubClient.getRepository("octocat","Hello_World")).thenReturn(fakeRepo("master"));
        when(githubClient.getBranchCommit("octocat","Hello_World", "master"))
                .thenReturn(new GithubCommit("commit123"));
        when(githubClient.getTree("octocat","Hello_World","commit123"))
                .thenReturn(new GithubTree("tree1", false, List.of(
                        new GithubTree.Entry("README", "blob", "sha-readme", 12L),
                        new GithubTree.Entry("lib", "tree", "sha-folder", null),
                        new GithubTree.Entry("lib/app.rb", "blob", "sha-app", 40L)
                )));

        RepositoryFileResponse result = service.listFiles("octocat", "Hello_World");

        assertThat(result.commitSha()).isEqualTo("commit123");
        assertThat(result.defaultBranch()).isEqualTo("master");
        assertThat(result.files()).extracting(RepositoryFile::path).containsExactly("README", "lib/app.rb");
    }

    @Test
    void failsWhenGithubTruncatesTheTree() {
        when(githubClient.getRepository("octocat", "Hello-World")).thenReturn(fakeRepo("master"));
        when(githubClient.getBranchCommit("octocat", "Hello-World", "master"))
                .thenReturn(new GithubCommit("commit123"));
        when(githubClient.getTree("octocat", "Hello-World", "commit123"))
                .thenReturn(new GithubTree("tree1", true, List.of()));

        assertThatThrownBy(() -> service.listFiles("octocat", "Hello-World"))
                .isInstanceOf(GithubApiException.class);
    }

    @Test
    void failsWhenRepositoryHasNoDefaultBranch() {
        when(githubClient.getRepository("octocat", "Hello-World")).thenReturn(fakeRepo(null));

        assertThatThrownBy(() -> service.listFiles("octocat", "Hello-World"))
                .isInstanceOf(GithubApiException.class);
        verify(githubClient, never()).getBranchCommit(any(), any(), any());
    }

    @Test
    void rejectsRepositoryNameStartingWithDot() {
        assertThatThrownBy(() -> service.listFiles("octocat", ".hidden"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(githubClient);
    }
}
