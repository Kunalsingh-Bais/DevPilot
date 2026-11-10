package com.chat.service.service;

import com.chat.service.answer.CitationValidator;
import com.chat.service.client.IndexingServiceClient;
import com.chat.service.client.IndexingServiceClient.RepositoryStatus;
import com.chat.service.client.IndexingServiceClient.SearchHit;
import com.chat.service.dto.AskResponse;
import com.chat.service.dto.AskResponse.Citation;
import com.chat.service.llm.LlmClient;
import com.chat.service.prompt.LabeledChunk;
import com.chat.service.prompt.PromptBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// The question-answering flow. It holds the rules; the controller only passes requests in.
@Service
public class AnswerService {

    // How many chunks we give the model
    static final int TOP_K = 8;

    static final int MAX_QUESTION_CHARS = 1000;

    private final IndexingServiceClient indexingServiceClient;
    private final LlmClient llmClient;

    // Constructor injection
    public AnswerService(IndexingServiceClient indexingServiceClient, LlmClient llmClient) {
        this.indexingServiceClient = indexingServiceClient;
        this.llmClient = llmClient;
    }

    public AskResponse ask(String githubOwner, String repoName, String question) {
        // Step 1: Check the input before doing any expensive work
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be empty");
        }

        if (question.length() > MAX_QUESTION_CHARS) {
            throw new IllegalArgumentException("question must be " + MAX_QUESTION_CHARS + " characters or fewer");
        }

        String trimmed = question.trim();

        // Step 2: Check repository indexing status
        RepositoryStatus status = indexingServiceClient.getStatus(githubOwner, repoName);

        if (!"COMPLETED".equals(status.status())) {
            throw new IllegalStateException(
                    "Repository is not ready for questions. Current status: " + status.status());
        }

        // Step 3: find the closest code chunks
        List<SearchHit> hits = indexingServiceClient.search(githubOwner, repoName, trimmed, TOP_K);

        // if nothing was found, say so without calling the model
        if (hits.isEmpty()) {
            return new AskResponse(PromptBuilder.NOT_FOUND_TEXT, status.commitSha(), List.of());
        }

        // Step 4: label the chunks C1, C2, ... in the order they were found
        List<LabeledChunk> labeled = new ArrayList<>();
        for (int i=0; i < hits.size(); i++) {
            labeled.add(new LabeledChunk("C" + (i + 1), hits.get(i)));
        }

        // Step 5: Build the prompt ask the model
        PromptBuilder.Prompt prompt = PromptBuilder.build(trimmed, labeled);
        String rawAnswer = llmClient.generate(prompt.system(), prompt.user());

        // Step 6: Remove any citation label we did not give the model
        List<String> allowedLabels = labeled.stream().map(LabeledChunk::label).toList();
        CitationValidator.Result validated = CitationValidator.validate(rawAnswer, allowedLabels);
        
        // Step 7: turn each used label into a link to the exact lines on GitHub
        List<Citation> citations = new ArrayList<>();
        for (String label : validated.usedLabels()) {
            SearchHit hit = findByLabel(labeled, label);
            citations.add(new Citation(
                    label,
                    hit.filePath(),
                    hit.startLine(),
                    hit.endLine(),
                    githubUrl(githubOwner, repoName, status.commitSha(), hit)));
        }

        return new AskResponse(validated.text(), status.commitSha(), citations);
    }

    // Finds the chunk that carries a given label. The label always exists, because we validated it.
    private static SearchHit findByLabel(List<LabeledChunk> labeled, String label) {
        return labeled.stream()
                .filter(l -> l.label().equals(label))
                .findFirst()
                .orElseThrow()
                .chunk();
    }

    // Builds a GitHub link to the lines, using the commit we indexed.
    private static String githubUrl(String githubOwner, String repoName, String commitSha, SearchHit hit) {
        return "https://github.com/%s/%s/blob/%s/%s#L%d-L%d".formatted(
                githubOwner, repoName, commitSha, hit.filePath(), hit.startLine(), hit.endLine());
    }
}
