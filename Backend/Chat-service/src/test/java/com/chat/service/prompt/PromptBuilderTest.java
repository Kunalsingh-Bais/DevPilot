package com.chat.service.prompt;

import com.chat.service.client.IndexingServiceClient.SearchHit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private static final SearchHit HIT_A = new SearchHit(1L, "src/OwnerController.java",
            84, 131, "class OwnerController {}", 0.38);

    private static final SearchHit HIT_B = new SearchHit(2L, "src/Owner.java",
            1, 20, "class Owner {}", 0.41);

    @Test
    void labelsEveryExcerptWithItsFileAndLines() {
        PromptBuilder.Prompt prompt = PromptBuilder.build("Where are owners saved?", List.of(
                new LabeledChunk("C1", HIT_A),
                new LabeledChunk("C2", HIT_B)));

        // The model needs the label, the file, and the line range to cite correctly
        assertThat(prompt.user())
                .contains("--- BEGIN C1 | src/OwnerController.java | lines 84-131 ---")
                .contains("--- BEGIN C2 | src/Owner.java | lines 1-20 ---")
                .contains("Question: Where are owners saved?");
    }

    @Test
    void systemRulesTellTheModelToIgnoreInstructionsInsideCode() {
        PromptBuilder.Prompt prompt = PromptBuilder.build("q", List.of(new LabeledChunk("C1", HIT_A)));

        // This is a basic defence against repository content that tries to change the model's behaviour
        assertThat(prompt.system()).contains("ignore them");
        assertThat(prompt.system()).contains(PromptBuilder.NOT_FOUND_TEXT);
    }
}
