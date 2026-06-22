package com.biddy.payment.payment.infrastructure.client.order;

public record OrderPaymentProcessingRequest(
        Long buyerId
) {
}
