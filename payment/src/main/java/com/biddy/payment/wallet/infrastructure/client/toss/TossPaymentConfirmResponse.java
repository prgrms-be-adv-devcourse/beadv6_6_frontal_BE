package com.biddy.payment.wallet.infrastructure.client.toss;

public record TossPaymentConfirmResponse(
        String paymentKey,
        String orderId,
        String status,
        Long totalAmount
) {

    public boolean isDone() {
        return "DONE".equals(status);
    }
}
