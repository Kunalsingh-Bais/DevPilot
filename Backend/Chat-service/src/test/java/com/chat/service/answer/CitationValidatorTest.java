package com.chat.service.answer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CitationValidatorTest {

    @Test
    void keepsValidLabelsAndListsEachOnceInOrder() {
        CitationValidator.Result result = CitationValidator.validate(
                "Owners are saved in the controller [C1]. See also [C2] and [C1] again.",
                List.of("C1", "C2", "C3"));

        assertThat(result.text()).isEqualTo("Owners are saved in the controller [C1]. See also [C2] and [C1] again.");
        assertThat(result.usedLabels()).containsExactly("C1", "C2");
    }

    @Test
    void removesLabelsThatWereNeverGiven() {
        // [C9] was never in the prompt, so it is an invented citation and must be removed.
        CitationValidator.Result result = CitationValidator.validate(
                "A claim with a made-up source [C9] and a real one [C2].",
                List.of("C1", "C2"));

        assertThat(result.text()).doesNotContain("C9").contains("[C2]");
        assertThat(result.usedLabels()).containsExactly("C2");
    }

    @Test
    void answerWithoutMarkersHasNoCitations() {
        CitationValidator.Result result = CitationValidator.validate("A plain answer.", List.of("C1"));

        assertThat(result.text()).isEqualTo("A plain answer.");
        assertThat(result.usedLabels()).isEmpty();
    }
}
