package com.indexing.service.embedding;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

// Test 1:- Fake model,so it runs without Ollama.
@ExtendWith(MockitoExtension.class)
class OllamaEmbeddingProviderTest {

    // A fake Spring AI model, so these tests don't need Ollama.
    @Mock
    private EmbeddingModel embeddingModel;

    @Test
    void emptyInputReturnsEmptyWithoutCallingTheModel() {
        OllamaEmbeddingProvider provider = new OllamaEmbeddingProvider(embeddingModel);

        assertThat(provider.embed(List.of())).isEmpty();
        verifyNoInteractions(embeddingModel);
    }

    @Test
    void returnsVectorsFromTheModel() {
        // The fake model returns one 768-number vector per text.
        when(embeddingModel.embed(anyList()))
                .thenReturn(List.of(new float[768], new float[768]));
        OllamaEmbeddingProvider provider = new OllamaEmbeddingProvider(embeddingModel);

        List<float[]> result = provider.embed(List.of("first", "second"));

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).hasSize(768);
    }

    @Test
    void rejectsWrongVectorSize() {
        // The fake model returns 10 numbers. The provider must refuse it.
        when(embeddingModel.embed(anyList())).thenReturn(List.of(new float[10]));

        OllamaEmbeddingProvider provider = new OllamaEmbeddingProvider(embeddingModel);

        assertThatThrownBy(() -> provider.embed(List.of("text")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("768");
        verify(embeddingModel, never()).embed(List.of());
    }
 }

