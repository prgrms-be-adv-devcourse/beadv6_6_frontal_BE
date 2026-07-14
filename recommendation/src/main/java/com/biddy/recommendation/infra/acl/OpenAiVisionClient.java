package com.biddy.recommendation.infra.acl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiVisionClient {

    private static final String DESCRIBE_PROMPT = """
            이 이미지에 있는 상품을 분석해서 아래 JSON 형식으로만 답변해줘. 다른 텍스트는 붙이지 마.
            {
              "category": "신발, 노트북, 가방, 의류, 전자기기, 여행용품, 기타 중 하나만 골라서 적어",
              "description": "상품 종류, 브랜드(추정 가능하면), 색상, 특징을 담은 한두 문장 설명 (한국어)"
            }
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.vision-model}")
    private String model;

    @Value("${openai.base-url}")
    private String baseUrl;

    public ImageDescription describeImage(byte[] imageBytes, String contentType) {
        String dataUri = "data:%s;base64,%s".formatted(contentType, Base64.getEncoder().encodeToString(imageBytes));

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "text", "text", DESCRIBE_PROMPT),
                                Map.of("type", "image_url", "image_url", Map.of("url", dataUri))
                        )
                )),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0,
                "max_tokens", 200
        );

        OpenAiVisionResponse response = restClient.post()
                .uri(baseUrl + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(OpenAiVisionResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("OpenAI Vision 응답이 비어있습니다.");
        }

        String content = response.choices().get(0).message().content();
        try {
            return objectMapper.readValue(content, ImageDescription.class);
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI Vision 응답을 파싱할 수 없습니다: " + content, e);
        }
    }
}
