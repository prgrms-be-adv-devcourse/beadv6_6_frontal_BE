package com.biddy.payment.payment.infrastructure.client.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OrderClient {

    private final RestClient restClient;

    public OrderClient(
            RestClient.Builder restClientBuilder,
            @Value("${biddy.client.order-service-url:http://order-service}") String orderServiceUrl
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

    public void requestPaymentProcessing(Long orderId) {
        restClient.patch()
                .uri("/api/orders/{orderId}/payment-processing", orderId)
                .retrieve()
                .toBodilessEntity();
    }
}
