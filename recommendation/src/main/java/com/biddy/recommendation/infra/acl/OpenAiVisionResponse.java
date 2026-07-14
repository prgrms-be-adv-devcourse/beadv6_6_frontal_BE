package com.biddy.recommendation.infra.acl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// OpenAI 실제 응답엔 id/object/created/model/usage(top)/index/logprobs/finish_reason(choice)/role(message)
// 같은 필드가 더 있어서, 안 쓰는 필드는 무시하도록 해야 함 (안 그러면 Jackson이 "모르는 필드" 예외를 던짐)
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiVisionResponse(
        List<Choice> choices
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String content) {
    }
}
