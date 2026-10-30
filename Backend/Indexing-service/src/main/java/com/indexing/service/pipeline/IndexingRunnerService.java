package com.indexing.service.pipeline;

import com.indexing.service.chunking.CodeChunker;
import com.indexing.service.chunking.TextChunk;
import com.indexing.service.embedding.EmbeddingProvider;
import com.indexing.service.filter.FileFilter;
import com.indexing.service.repoClient.RepoFileContent;
import com.indexing.service.repoClient.RepoFileEntry;
import com.indexing.service.repoClient.RepoFileListing;
import com.indexing.service.repoClient.RepositoryServiceClient;
import com.indexing.service.service.IndexingJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Runs the whole indexing pipeline for one repository, in the background.
@Service
public class IndexingRunnerService {

    private static final Logger log = LoggerFactory.getLogger(IndexingRunnerService.class);

    // Limits for one job
    static final int MAX_FILES = 500;
    static final int MAX_CHUNKS = 2000;

    // Chunk tests go to embedding model in one call
    static final int EMBED_BATCH_SIZE = 32;

    private final RepositoryServiceClient repositoryServiceClient;
    private final EmbeddingProvider embeddingProvider;
    private final ChunkStore chunkStore;
    private final IndexingJobService indexingJobService;

    // Constructor injection
    public IndexingRunnerService(RepositoryServiceClient repositoryServiceClient,
                                 EmbeddingProvider embeddingProvider,
                                 ChunkStore chunkStore,
                                 IndexingJobService indexingJobService) {
        this.repositoryServiceClient = repositoryServiceClient;
        this.embeddingProvider = embeddingProvider;
        this.chunkStore = chunkStore;
        this.indexingJobService = indexingJobService;
    }

// ------ Method 1: Start indexing ------
    @Async
    public void run(long repositoryId, String githubOwner, String repoName) {
        log.info("Indexing started for {}/{} (id {})", githubOwner, repoName, repositoryId);

        try {
            // Step 1: list the files at the latest commit
            RepoFileListing listing = repositoryServiceClient.listFiles(githubOwner, repoName);

            // Steps 2 to 4: filter, read, and chunk
            List<SourceChunk> chunks = readChunks(githubOwner, repoName, listing);

            // Step 5: turn each chunk into a vector, in batches
            List<EmbeddedChunk> embedded = embedInBatches(chunks);

            // Step 5: replace the old chunks, then record the commit as indexed
            chunkStore.replaceChunks(repositoryId, listing.commitSha(), embedded);
            indexingJobService.markCompleted(repositoryId, listing.commitSha());

            log.info("Indexing completed for {}/{}: {} chunks", githubOwner, repoName, embedded);
        }
        catch (RuntimeException ex) {
            log.warn("Indexing failed for {}/{}: {}", githubOwner, repoName, ex.toString());

            indexingJobService.markFailed(repositoryId, describe(ex));
        }
    }

// ------ Method 2: Filters the files, reads and split into chunks ------
     private List<SourceChunk> readChunks(String githubOwner, String repoName, RepoFileListing listing) {
        if (listing.files().size() > MAX_FILES) {
            throw new IllegalStateException("Repository has " + listing.files().size() +
                    " files, which is above the limit of " + MAX_FILES + ".");
        }

        List<SourceChunk> chunks = new ArrayList<>();

        for (RepoFileEntry file : listing.files()) {
            // Decide from the path and size first, so skipped file costs no GitHub request.
            if (!FileFilter.evaluate(file.path(), file.size()).keep()) {
                continue;
            }

            // Get the content of file
            Optional<RepoFileContent> content = repositoryServiceClient.getFileContent(githubOwner, repoName, file.sha());

            // Check if content is empty , if empty skip it
            if (content.isEmpty()) {
                continue;
            }

            for (TextChunk piece: CodeChunker.split(content.get().content())) {
                if (chunks.size() >= MAX_CHUNKS) {
                    throw new IllegalStateException("Repository produces more than " + MAX_CHUNKS + " chunks, which is above the limit.");
                }

                chunks.add(new SourceChunk(file.path(), piece.startLine(), piece.endLine(), piece.content()));
            }
        }
        return chunks;
     }

// ------ Method 3: Sends the chunks texts to the embedding model in batches ------
    // then pairs each vector with its chunk
    private List<EmbeddedChunk> embedInBatches(List<SourceChunk> chunks) {
        List<EmbeddedChunk> result = new ArrayList<>();

        for (int from = 0; from < chunks.size();from += EMBED_BATCH_SIZE) {
            int to = Math.min(from + EMBED_BATCH_SIZE, chunks.size());

            List<SourceChunk> batch = chunks.subList(from, to);

            List<String> texts = batch.stream().map(SourceChunk::content).toList();
            List<float[]> vectors = embeddingProvider.embed(texts);

            // Each text must get exactly one vector, Otherwise the pairs would be wrong
            if (vectors.size() != batch.size()) {
                throw new IllegalStateException("Embedding model returned " + vectors.size() +
                        " vectors for " + batch.size() + " chunks.");
            }

            for (int i=0; i<batch.size(); i++) {
                result.add(new EmbeddedChunk(batch.get(i), vectors.get(i)));
            }
        }
        return result;
    }

    private static String describe(RuntimeException ex) {
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    }
}
