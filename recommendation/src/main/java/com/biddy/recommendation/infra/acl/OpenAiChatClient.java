package com.biddy.recommendation.infra.acl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiChatClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.vision-model}")
    private String model;

    @Value("${openai.base-url}")
    private String baseUrl;

    public ChecklistSuggestion generateChecklistSuggestion(String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.3,
                "max_tokens", 400
        );

        OpenAiVisionResponse response = restClient.post()
                .uri(baseUrl + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(OpenAiVisionResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("OpenAI 응답이 비어있습니다.");
        }

        String content = response.choices().get(0).message().content();
        try {
            return objectMapper.readValue(content, ChecklistSuggestion.class);
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI 응답을 파싱할 수 없습니다: " + content, e);
        }
    }
}
