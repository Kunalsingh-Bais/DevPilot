package com.repository.service;

import com.repository.service.client.GithubClient;
import com.repository.service.dto.RepositorySummary;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RepositoryServiceTest {

    // A fake GitHub client, so the tests never call the real GitHub
    @Mock
    private GithubClient githubClient;

    // The real service, with the fake client passed in
    @InjectMocks
    private RepositoryService service;

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
}
