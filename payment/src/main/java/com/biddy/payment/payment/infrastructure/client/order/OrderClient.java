package com.biddy.payment.payment.infrastructure.client.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OrderClient {

    private final RestClient restClient;

    public OrderClient(
            RestClient.Builder restClientBuilder,
            @Value("${biddy.client.order-service-url:http://localhost:8083}") String orderServiceUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(orderServiceUrl)
                .build();
    }

    public OrderPaymentInfo getPaymentInfo(Long orderId) {
        return restClient.get()
                .uri("/api/orders/{orderId}/payment-info", orderId)
                .retrieve()
                .body(OrderPaymentInfo.class);
    }

    public void requestPaymentProcessing(Long orderId, Long buyerId, String memberRole) {
        restClient.patch()
                .uri("/api/orders/{orderId}/payment-processing", orderId)
                .header("X-Member-Id", String.valueOf(buyerId))
                .header("X-Member-Role", memberRole)
                .retrieve()
                .toBodilessEntity();
    }
}
