package com.biddy.recommendation.infra.acl;

import java.util.List;

public record OpenAiVisionResponse(
        List<Choice> choices
) {
    public record Choice(Message message) {
    }

    public record Message(String content) {
    }
}
