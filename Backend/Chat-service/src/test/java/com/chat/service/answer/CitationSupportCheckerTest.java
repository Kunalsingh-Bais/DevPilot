package com.chat.service.answer;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CitationSupportCheckerTest {

    private static final String README_CHUNK = "Development server, code scaffolding, building, unit tests and end-to-end tests.";

    private static final String REPOSITORY_CHUNK = "OwnerRepository saves each owner with the repository save method.";

    @Test
    void removeCitationWhenSentenceSharesNoWordsWithChunk() {
        CitationSupportChecker.Result result = CitationSupportChecker.check(
                "The class represents a pet [C1].", Map.of("C1", README_CHUNK));

        assertThat(result.text()).isEqualTo("The class represents a pet.");
        assertThat(result.keptLabels()).isEmpty();
    }

    @Test
    void keepsCitationWhenSentenceSharesWordsWithChunk() {
        // "repository", "save", and "method" appear in the chunk, so the citation stays.
        CitationSupportChecker.Result result = CitationSupportChecker.check(
                "Owners are saved through the repository save method [C1].",
                Map.of("C1", REPOSITORY_CHUNK));

        assertThat(result.text()).contains("[C1]");
        assertThat(result.keptLabels()).containsExactly("C1");
    }

    @Test
    void checksEachSentenceSeparately() {
        // The first sentence is supported and keeps its citation. The second is not, so its citation is removed.
        CitationSupportChecker.Result result = CitationSupportChecker.check(
                "Owners are saved through the repository save method [C1]. The class represents a pet [C1].",
                Map.of("C1", REPOSITORY_CHUNK));

        assertThat(result.text()).contains("repository save method [C1].");
        assertThat(result.text()).doesNotContain("pet [C1]");
        assertThat(result.keptLabels()).containsExactly("C1");
    }

    @Test
    void leavesSentencesWithoutCitationsUnchanged() {
        CitationSupportChecker.Result result = CitationSupportChecker.check(
                "This is a plain sentence. It has no citation.", Map.of());

        assertThat(result.text()).isEqualTo("This is a plain sentence. It has no citation.");
        assertThat(result.keptLabels()).isEmpty();
    }
}
