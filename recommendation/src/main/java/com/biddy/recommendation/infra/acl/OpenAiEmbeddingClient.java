package com.biddy.recommendation.infra.acl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiEmbeddingClient {

    private final RestClient restClient;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.embedding-model}")
    private String model;

    @Value("${openai.base-url}")
    private String baseUrl;

    public float[] embed(String text) {
        OpenAiEmbeddingResponse response = restClient.post()
                .uri(baseUrl + "/embeddings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", model, "input", text))
                .retrieve()
                .body(OpenAiEmbeddingResponse.class);

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new IllegalStateException("OpenAI 임베딩 응답이 비어있습니다.");
        }
        return response.data().get(0).embedding();
    }
}
