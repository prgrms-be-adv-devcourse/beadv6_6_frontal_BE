package com.biddy.productservice.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 임베딩 저장 요청 (recommendation-service 내부 전용)")
public record ProductEmbeddingUpsertRequest(
        @Schema(description = "임베딩 벡터 (text-embedding-3-small, 1536차원)")
        float[] embedding,

        @Schema(description = "임베딩에 사용된 원본 텍스트")
        String sourceText,

        @Schema(description = "상품 카테고리")
        String category
) {
}
