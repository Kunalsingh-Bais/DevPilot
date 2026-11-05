package com.indexing.service.retrieval;

import com.indexing.service.embedding.EmbeddingProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ChunkRetrieverTest {

    // Empty questions must not cost an embedding call.
    @Mock
    private EmbeddingProvider embeddingProvider;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ChunkRetrieverService chunkRetrieverService;

    @Test
    void rejectsBlankQuestionsBeforeEmbedding() {
        assertThatThrownBy(() -> chunkRetrieverService.search("spring-projects", "spring-petclinic", "  ", 5))
                .isInstanceOf(IllegalArgumentException.class);
        // Empty questions must not cost an embedding call.
        verifyNoInteractions(embeddingProvider);
    }

    @Test
    void rejectsLimitAboveMaximum() {
        assertThatThrownBy(() -> chunkRetrieverService.search("spring-projects", "spring-petclinic", "login", 21))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(jdbcTemplate);
    }
}
