package com.biddy.recommendation.infra.acl;

public record ProductEmbeddingUpsertRequest(
        float[] embedding,
        String sourceText,
        String category
) {
}
