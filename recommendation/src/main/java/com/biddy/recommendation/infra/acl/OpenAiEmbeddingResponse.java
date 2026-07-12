package com.biddy.recommendation.infra.acl;

import java.util.List;

public record OpenAiEmbeddingResponse(
        List<EmbeddingItem> data
) {
    public record EmbeddingItem(float[] embedding) {
    }
}
