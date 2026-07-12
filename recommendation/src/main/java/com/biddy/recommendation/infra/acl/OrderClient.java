package com.biddy.recommendation.infra.acl;

import com.biddy.recommendation.application.dto.CartItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderClient {

    private final RestClient restClient;

    @Value("${order.service-url}")
    private String baseUrl;

    public List<CartItemDto> getCartItems(Long memberId) {
        CartItemDto[] items = restClient.get()
                .uri(baseUrl + "/api/cart/list")
                .header("X-Member-Id", String.valueOf(memberId))
                .retrieve()
                .body(CartItemDto[].class);
        return items == null ? List.of() : Arrays.asList(items);
    }
}
