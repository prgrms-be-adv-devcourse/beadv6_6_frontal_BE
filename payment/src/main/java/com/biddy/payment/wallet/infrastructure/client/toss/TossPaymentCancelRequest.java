package com.biddy.payment.wallet.infrastructure.client.toss;

public record TossPaymentCancelRequest(
        String cancelReason,
        Long cancelAmount
) {
}
