package com.biddy.recommendation.infra.acl;

import com.biddy.recommendation.application.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
}
