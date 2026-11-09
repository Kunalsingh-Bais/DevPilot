package com.chat.service.service;

import com.chat.service.client.IndexingServiceClient;
import com.chat.service.client.IndexingServiceClient.RepositoryStatus;
import com.chat.service.client.IndexingServiceClient.SearchHit;
import com.chat.service.dto.AskResponse;
import com.chat.service.llm.LlmClient;
import com.chat.service.prompt.PromptBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnswerServiceTest {

    private static final String GITHUB_OWNER = "spring-projects";
    private static final String REPO_NAME = "spring-petclinic";
    private static final String SHA = "a".repeat(40);
    private static final String QUESTION = "Where are owners saved?";

    // A fake indexing-service client. No network is used.
    @Mock
    private IndexingServiceClient indexingServiceClient;

    @Test
    void rejectsRepositoryThatIsNotCompleted() {
        when(indexingServiceClient.getStatus(GITHUB_OWNER, REPO_NAME))
                .thenReturn(new RepositoryStatus(1L, GITHUB_OWNER, REPO_NAME, "RUNNING", null, null));

        AnswerService service = new AnswerService(indexingServiceClient, (systemRules, userMessage) -> "unused");

        assertThatThrownBy(() -> service.ask(GITHUB_OWNER, REPO_NAME, QUESTION))
                .isInstanceOf(IllegalStateException.class);

        verify(indexingServiceClient, never()).search(any(), any(), any(), anyInt());
    }

    @Test
    void returnsNotFoundWithoutCallingTheModelWhenNothingMatches() {
        when(indexingServiceClient.getStatus(GITHUB_OWNER, REPO_NAME))
                .thenReturn(new RepositoryStatus(1L, GITHUB_OWNER, REPO_NAME, "COMPLETED", SHA, null));

        when(indexingServiceClient.search(GITHUB_OWNER, REPO_NAME, QUESTION, 5))
                .thenReturn(List.of());

        AtomicBoolean modelCalled = new AtomicBoolean(false);
        AnswerService service = new AnswerService(indexingServiceClient, (systemRules, userMessage) -> {
            modelCalled.set(true);
            return "should not be used";
        });

        AskResponse response = service.ask(GITHUB_OWNER, REPO_NAME, QUESTION);

        assertThat(response.answer()).isEqualTo(PromptBuilder.NOT_FOUND_TEXT);
        assertThat(response.citations()).isEmpty();
        assertThat(modelCalled).isFalse();
    }

    @Test
    void removesInventedCitationsAndLinksRealOnesToTheCommit() {
        when(indexingServiceClient.getStatus(GITHUB_OWNER, REPO_NAME))
                .thenReturn(new RepositoryStatus(1L, GITHUB_OWNER, REPO_NAME, "COMPLETED", SHA, null));

        when(indexingServiceClient.search(GITHUB_OWNER, REPO_NAME, QUESTION, 5)).thenReturn(List.of(
                new SearchHit(1L, "src/OwnerController.java", 84, 131, "code A", 0.38),
                new SearchHit(2L, "src/OwnerRepository.java", 1, 39, "code B", 0.36)));

        // A fake model that cites one real label and one invented label.
        LlmClient fakeModel = ((systemRules, userMessage) ->
                "Owners are saved in the controller [C1]. An invented source [C9].");
        AnswerService service = new AnswerService(indexingServiceClient, fakeModel);

        AskResponse response = service.ask(GITHUB_OWNER, REPO_NAME, QUESTION);

        assertThat(response.answer()).contains("[C1]").doesNotContain("[C9]");
        assertThat(response.commitSha()).isEqualTo(SHA);
        assertThat(response.citations()).hasSize(1);
        assertThat(response.citations().get(0).githubUrl()).isEqualTo(
                "https://github.com/spring-projects/spring-petclinic/blob/" + SHA
                        + "/src/OwnerController.java#L84-L131");
    }
}
