package com.repository.service;

import com.repository.service.client.GithubBlob;
import com.repository.service.client.GithubClient;
import com.repository.service.dto.RepositoryFileContent;
import com.repository.service.exception.GithubApiException;
import com.repository.service.service.FileContentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileContentServiceTest {

    // A valid blob SHA
    private static final String SHA = "a".repeat(40);

    // A fake GitHub client
    @Mock
    private GithubClient githubClient;

    @InjectMocks
    private FileContentService service;

    // Encodes text the same way GitHub does
    private String base64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void decodesTextFile() {
        String text = "public class App {}\n";
        when(githubClient.getBlob("octocat", "Hello-World", SHA))
                .thenReturn(new GithubBlob(SHA, text.getBytes(StandardCharsets.UTF_8).length,
                        base64(text), "base64"));

        RepositoryFileContent result = service.getFileContent("octocat", "Hello-World", SHA);

        assertThat(result.content()).isEqualTo(text);
        assertThat(result.sha()).isEqualTo(SHA);
    }

    @Test
    void decodesBase64ThatContainsLineBreaks() {
        // GitHub adds line breaks inside long base64 text. "aGVs" is "hel" and "bG8=" is "lo".
        when(githubClient.getBlob("octocat", "Hello-World", SHA))
                .thenReturn(new GithubBlob(SHA, 5, "aGVs\nbG8=","base64"));

        RepositoryFileContent result = service.getFileContent("octocat", "Hello-World", SHA);

        assertThat(result.content()).isEqualTo("hello");
    }

    @Test
    void rejectsFileLargerThanLimit() {
        // The size is over 200KB, so the service must refuse it.
        when(githubClient.getBlob("octocat", "Hello-World", SHA))
                .thenReturn(new GithubBlob(SHA, 300_000, "", "base64"));

        assertThatThrownBy(() -> service.getFileContent("octocat", "Hello-World", SHA))
                .isInstanceOf(GithubApiException.class);
    }

    @Test
    void rejectsBinaryFile() {
        // 0 is a zero byte, Its presence marks the content as binary.
        byte[] binary = {80, 75, 0, 3, 4};
        when(githubClient.getBlob("octocat", "Hello-World", SHA))
                .thenReturn(new GithubBlob(SHA, binary.length,
                        Base64.getEncoder().encodeToString(binary), "base64"));

        assertThatThrownBy(() -> service.getFileContent("octocat", "Hello-World", SHA))
                .isInstanceOf(GithubApiException.class);
    }

    @Test
    void rejectsInvalidShaWithoutCallingGithub() {
        // "../../secret" could change the URL we send to GitHub, so it must never get that far.
        assertThatThrownBy(() -> service.getFileContent("octocat", "Hello-World", "../../secret"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(githubClient);
    }


}
