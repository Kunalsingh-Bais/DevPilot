package com.chat.service.prompt;

import com.chat.service.client.IndexingServiceClient.SearchHit;

// One code chunk with a short label, such as "C1", that the model can cite
public record LabeledChunk(
        String label,
        SearchHit chunk
) {
}
