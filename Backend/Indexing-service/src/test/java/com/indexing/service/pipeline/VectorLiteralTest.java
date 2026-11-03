package com.indexing.service.pipeline;

import org.junit.jupiter.api.Test;
import com.indexing.service.embedding.VectorText;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VectorLiteralTest {

    @Test
    void formatsNumbersInSquareBrackets() {
        assertThat(VectorText.toLiteral(new float[]{1f, 2.5f, -0.25f}))
                .isEqualTo("[1.0,2.5,-0.25]");
    }

    @Test
    void rejectsNotANumber() {
        assertThatThrownBy(() -> VectorText.toLiteral(new float[]{Float.NaN}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
