package com.biddy.recommendation.infra.acl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// OpenAI 실제 응답엔 object/model/usage 등 우리가 안 쓰는 필드가 더 있어서, 안 쓰는 필드는
// 무시하도록 해야 함 (안 그러면 Jackson이 "모르는 필드" 예외를 던져서 파싱 자체가 실패함)
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiEmbeddingResponse(
        List<EmbeddingItem> data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmbeddingItem(float[] embedding) {
    }
}
