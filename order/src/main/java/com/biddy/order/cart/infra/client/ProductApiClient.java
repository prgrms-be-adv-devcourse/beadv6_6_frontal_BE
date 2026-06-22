package com.biddy.order.cart.infra.client;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
@Component
@RequiredArgsConstructor
public class ProductApiClient {

    private final RestTemplate restTemplate;

    @Value("${product-service.url}")
    private String productServiceUrl;
    public List<ProductResponse> getProductsBulk(List<UUID> productIds) {
        String url = productServiceUrl + "/api/v1/products/bulk";
        // RestTemplate을 이용해 POST 호출
        ProductResponse[] response = restTemplate.postForObject(url, productIds, ProductResponse[].class);
        if (response == null) {
            return List.of();
        }
        return Arrays.asList(response);
    }
    // 상품 정보 응답 DTO
    public record ProductResponse(
            UUID productId,
            String name,
            BigDecimal price,
            String status,
            Long userId
    ) {}
}
