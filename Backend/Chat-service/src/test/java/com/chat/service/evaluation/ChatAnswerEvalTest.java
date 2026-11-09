package com.chat.service.evaluation;

import com.chat.service.dto.AskRequest;
import com.chat.service.dto.AskResponse;
import com.chat.service.prompt.PromptBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Tests the whole answer path against a running chat-service.
// It passes a question only when the expected file is cited in the answer.
class ChatAnswerEvalTest {

    private static final String BASE_URL = "http://localhost:8083";
    private static final String OWNER = "spring-projects";
    private static final String REPO = "spring-petclinic";

    // The goal matches the retrieval eval. Change it only with a written reason.
    private static final int MIN_HITS = 8;

    @Test
    void answersCiteTheExpectedFile() throws IOException {
        // Skip unless the caller asked for the evaluation on the command line.
        assumeTrue(Boolean.getBoolean("chatEval"),
                "Run with -DchatEval=true to execute the chat evaluation.");
        assumeTrue(isUp(BASE_URL + "/actuator/health"),
                "chat-service is not running on port 8083, so the evaluation is skipped.");

        // Chat calls can take a minute or more on a CPU-heavy machine, so allow a long read timeout.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofMinutes(3));
        RestClient client = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(factory)
                .build();

        List<EvalCase> cases = loadCases();
        int hits = 0;
        StringBuilder report = new StringBuilder();

        for (EvalCase evalCase : cases) {
            String outcome;
            AskResponse response = null;
            String error = null;

            try {
                response = client.post()
                        .uri("/api/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new AskRequest(OWNER, REPO, evalCase.question()))
                        .retrieve()
                        .body(AskResponse.class);
            } catch (RestClientException ex) {
                // One failed question must not stop the whole run. Record it and continue.
                error = ex.getMessage();
            }

            if (response == null) {
                outcome = "ERROR";
            } else {
                boolean cited = response.citations().stream()
                        .anyMatch(c -> matches(c.filePath(), evalCase.expectedFile()));
                boolean notFound = response.answer().contains(PromptBuilder.NOT_FOUND_TEXT);

                if (cited) {
                    outcome = "HIT";
                    hits++;
                } else if (notFound) {
                    outcome = "NOT-FOUND";
                } else {
                    outcome = "MISS";
                }
            }

            // One block per question: the outcome, the expected file, what was cited, and the start of the answer.
            report.append(String.format("%-9s %s%n", outcome, evalCase.question()));
            report.append("          expected: ").append(evalCase.expectedFile()).append('\n');

            if (response != null) {
                String shortAnswer = response.answer().replace('\n', ' ');
                report.append("          answer:   ")
                        .append(shortAnswer.length() > 200 ? shortAnswer.substring(0, 200) + "..." : shortAnswer)
                        .append('\n');
                if (response.citations().isEmpty()) {
                    report.append("          cited:    (none)\n");
                } else {
                    for (AskResponse.Citation c : response.citations()) {
                        report.append("          cited:    ").append(c.label()).append(" -> ")
                                .append(c.filePath()).append(':').append(c.startLine())
                                .append('-').append(c.endLine()).append('\n');
                    }
                }
            } else {
                report.append("          error:    ").append(error).append('\n');
            }
            report.append('\n');
        }

        System.out.println("=== Chat evaluation report ===\n" + report);
        System.out.println("Score: " + hits + " of " + cases.size() + " cited the expected file.");

        // The report appears in the failure message, so you can see which questions missed and why.
        assertThat(hits)
                .as("hits (goal %d of %d)%n%s", MIN_HITS, cases.size(), report)
                .isGreaterThanOrEqualTo(MIN_HITS);
    }

    // A match is the exact file, or a path ending in "/" plus the expected name.
    // The "/" stops "PetOwner.java" from matching "Owner.java".
    private static boolean matches(String filePath, String expected) {
        return filePath.equals(expected) || filePath.endsWith("/" + expected);
    }

    // Checks whether a URL answers with 200. Never throws.
    private static boolean isUp(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            return connection.getResponseCode() == 200;
        } catch (IOException ex) {
            return false;
        }
    }

    // Reads chat/eval-questions.txt. Lines starting with "#" and empty lines are ignored.
    private static List<EvalCase> loadCases() throws IOException {
        List<EvalCase> cases = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("chat/evaluation-questions.txt").getInputStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                // Split only at the first "|", so questions may contain other symbols.
                String[] parts = line.split("\\|", 2);
                if (parts.length != 2) {
                    throw new IllegalStateException("Line must look like 'question | file': " + line);
                }
                cases.add(new EvalCase(parts[0].trim(), parts[1].trim()));
            }
        }
        return cases;
    }

    // One row of the evaluation set.
    private record EvalCase(String question, String expectedFile) {
    }
}
