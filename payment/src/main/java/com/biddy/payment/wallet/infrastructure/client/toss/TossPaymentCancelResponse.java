package com.biddy.payment.wallet.infrastructure.client.toss;

public record TossPaymentCancelResponse(
        String paymentKey,
        String orderId,
        String status,
        Long totalAmount
) {
}
