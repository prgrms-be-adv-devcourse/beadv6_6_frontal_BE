package com.biddy.recommendation.infra.acl;

import com.biddy.recommendation.application.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class ProductClient {

    private final RestClient restClient;

    @Value("${product.service-url}")
    private String baseUrl;

    public ProductDto getProduct(Long productId) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/api/products/{id}", productId)
                    .retrieve()
                    .body(ProductDto.class);
        } catch (RestClientException e) {
            return null;
        }
    }

    // product_embedding 테이블은 product-service가 소유 — recommendation은 계산만 하고
    // 저장은 이 API로 위임함 (recommendation은 직접 쓰기 안 함, 조회만 자체 datasource로 함)
    public void saveEmbedding(Long productId, float[] embedding, String sourceText, String category) {
        restClient.put()
                .uri(baseUrl + "/api/products/{id}/embedding", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ProductEmbeddingUpsertRequest(embedding, sourceText, category))
                .retrieve()
                .toBodilessEntity();
    }
}
