package com.biddy.payment.wallet.infrastructure.client.toss;

public record TossPaymentConfirmRequest(
        String paymentKey,
        String orderId,
        Long amount
) {
}
