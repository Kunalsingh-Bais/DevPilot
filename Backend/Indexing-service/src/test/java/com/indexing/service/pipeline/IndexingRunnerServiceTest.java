package com.indexing.service.pipeline;

import com.indexing.service.embedding.EmbeddingProvider;
import com.indexing.service.repoClient.RepoFileContent;
import com.indexing.service.repoClient.RepoFileEntry;
import com.indexing.service.repoClient.RepoFileListing;
import com.indexing.service.repoClient.RepositoryServiceClient;
import com.indexing.service.service.IndexingJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndexingRunnerServiceTest {

    private static final String SHA_README = "a".repeat(40);
    private static final String SHA_PNG = "b".repeat(40);
    private static final String COMMIT = "c".repeat(40);

    @Mock
    private RepositoryServiceClient repositoryServiceClient;

    @Mock
    private ChunkStore chunkStore;

    @Mock
    private IndexingJobService indexingJobService;

    private IndexingRunnerService indexingRunnerService;

    @BeforeEach
    void setup() {
        // A fake embedding model: one small vector per text. No Ollama needed for these tests.
        EmbeddingProvider fakeEmbedder = texts -> texts.stream().map(t -> new float[]{1f, 2f}).toList();
        indexingRunnerService = new IndexingRunnerService(repositoryServiceClient, fakeEmbedder, chunkStore, indexingJobService);
    }

    @Test
    void indexesKeepableFilesAndCompletesJob() {
        when(repositoryServiceClient.listFiles("octocat", "Hello-World")).thenReturn(new RepoFileListing(
                "octocat", "Hello-World", "main", COMMIT,
                List.of(new RepoFileEntry("README.md", SHA_README, 12),
                        new RepoFileEntry("logo.png", SHA_PNG, 900))
        ));
        when(repositoryServiceClient.getFileContent("octocat", "Hello-World", SHA_README))
                .thenReturn(Optional.of(new RepoFileContent(SHA_README, 12, "# Hello\n")));

        indexingRunnerService.run(1L, "octocat", "Hello-World");

        // The image was filtered out, so GitHub was never asked for it
        verify(repositoryServiceClient, never()).getFileContent(any(), any(), eq(SHA_PNG));

        // One chunk was saved, and the job was marked completed with the commit it came from.
        verify(chunkStore).replaceChunks(eq(1L), eq(COMMIT), argThat(list -> list.size() == 1));
        verify(indexingJobService).markCompleted(1L, COMMIT);
    }

    @Test
    void skipsFileThatRepoServiceRefusesAndStillCompletes() {
        when(repositoryServiceClient.listFiles("octocat", "Hello-World")).thenReturn(new RepoFileListing(
                "octocat", "Hello-World", "main", COMMIT,
                List.of(new RepoFileEntry("README.md", SHA_README, 12))));

        // repository-service said "skip this file" (for example, it is binary).
        when(repositoryServiceClient.getFileContent("octocat", "Hello-World", SHA_README))
                .thenReturn(Optional.empty());

        indexingRunnerService.run(1L, "octocat", "Hello-World");

        // No chunks, but the job still finishes. One bad file must not fail the whole repository.
        verify(chunkStore).replaceChunks(eq(1L), eq(COMMIT), argThat(list -> list.isEmpty()));
        verify(indexingJobService).markCompleted(1L, COMMIT);
    }

    @Test
    void failsJobWhenEmbeddingFails() {
        // An embedding model that always fails.
        EmbeddingProvider brokenEmbedder = texts -> {
            throw new IllegalStateException("model offline");
        };
        indexingRunnerService = new IndexingRunnerService(repositoryServiceClient, brokenEmbedder, chunkStore, indexingJobService);

        when(repositoryServiceClient.listFiles("octocat", "Hello-World")).thenReturn(new RepoFileListing(
                "octocat", "Hello-World", "main", COMMIT,
                List.of(new RepoFileEntry("README.md", SHA_README, 12))));
        when(repositoryServiceClient.getFileContent("octocat", "Hello-World", SHA_README))
                .thenReturn(Optional.of(new RepoFileContent(SHA_README, 12, "# Hello\n")));

        indexingRunnerService.run(1L, "octocat", "Hello-World");

        // The job is marked FAILED with the reason, and nothing is saved or marked completed.
        verify(indexingJobService).markFailed(1L, "model offline");
        verify(indexingJobService, never()).markCompleted(anyLong(), anyString());
        verify(chunkStore, never()).replaceChunks(anyLong(), anyString(), anyList());
    }

    @Test
    void rejectsRepositoryWithTooManyFiles() {
        // One more file than the limit allows.
        List<RepoFileEntry> tooMany = IntStream.range(0, IndexingRunnerService.MAX_FILES + 1)
                .mapToObj(i -> new RepoFileEntry("src/File" + i + ".java", SHA_README, 10))
                .toList();
        when(repositoryServiceClient.listFiles("octocat", "Hello-World")).thenReturn(new RepoFileListing(
                "octocat", "Hello-World", "main", COMMIT, tooMany));

        indexingRunnerService.run(1L, "octocat", "Hello-World");

        // The job fails before reading any file, so no GitHub request is wasted.
        verify(indexingJobService).markFailed(eq(1L), contains("limit"));
        verify(repositoryServiceClient, never()).getFileContent(any(), any(), any());
    }
}
