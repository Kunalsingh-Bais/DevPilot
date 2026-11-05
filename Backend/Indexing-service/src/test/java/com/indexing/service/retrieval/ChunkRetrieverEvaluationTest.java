package com.indexing.service.retrieval;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Measures retrieval quality against the real database and the real Ollama model.
// It reports HIT or MISS for each question, then checks that enough questions hit.
@SpringBootTest
class ChunkRetrieverEvaluationTest {

    private static final String GITHUB_OWNER = "spring-projects";
    private static final String REPO_NAME = "spring-petclinic";

    // A question is a hit when the expected file appears among the top results.
    private static final int TOP_K = 5;

    // The goal from plan: at least 8 of the questions must be hits.
    private static final int MIN_HITS = 8;

    @Autowired
    private ChunkRetrieverService chunkRetrieverService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void expectedFileAppearsInTopResults() throws IOException {
        // Skip, instead of failing, when the repository has not been indexed yet.
        Integer indexed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM code_chunks c JOIN repositories r ON r.id = c.repository_id "
                            + "WHERE r.github_owner = ? AND r.repo_Name = ?",
                Integer.class, GITHUB_OWNER, REPO_NAME);

        assumeTrue(indexed != null && indexed > 0, "spring-petclinic is not indexed yet, so the test is skipped.");

        List<EvalCase> cases = loadCases();
        int hits = 0;

        // The report lists every question, its result, and the chunks that were returned.
        StringBuilder report = new StringBuilder();

        for (EvalCase evalCase : cases) {
            List<RetrievedChunk> results = chunkRetrieverService.search(GITHUB_OWNER, REPO_NAME, evalCase.question(), TOP_K);

            int rank = 0;
            for (int i=0; i<results.size(); i++) {
                if (matches(results.get(i).filePath(), evalCase.expectedFile())) {
                    rank = i + 1;
                    break;
                }
            }
            boolean hit = rank >= 1 && rank <= TOP_K;

            if (hit) {
                hits++;
            }

            report.append(hit ? "HIT  " : "MISS " )
                    .append(evalCase.question())
                    .append("  (expected ").append(evalCase.expectedFile()).append(")\n")
                    .append(", rank ").append(rank == 0 ? "not in top 20" : String.valueOf(rank))
                    .append(")\n");

            for (RetrievedChunk r : results) {
                report.append(String.format("      %.3f  %s:%d-%d%n",
                        r.distance(), r.filePath(), r.startLine(), r.endLine()));
            }
            report.append('\n');
        }

        // The message shows the full report if the test fails, so we can see which questions missed.
        System.out.println("=== Retrieval report ===\n" + report);

        assertThat(hits)
                .as("hits in top %d (goal %d of %d)%n%s", TOP_K, MIN_HITS, cases.size(), report)
                .isGreaterThanOrEqualTo(MIN_HITS);
    }

// ------ A match is the exact file, or a file whose path ends with "/" plus the expected name. ------
    private static boolean matches(String filePath, String expected) {
        return filePath.equals(expected) || filePath.endsWith("/" + expected);
    }

// ------ Reads the evaluation file. Lines starting with "#" and empty lines are ignored ------
    private static List<EvalCase> loadCases() throws IOException {
        List<EvalCase> cases = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("retrieval/evaluation-questions.txt").getInputStream(),
                StandardCharsets.UTF_8
        ))) {
            String line;
            while((line = reader.readLine()) != null) {
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

// ------ One row of the evaluation set: a question and the file that should answer it. ------
    private record EvalCase(String question, String expectedFile) {}
}
