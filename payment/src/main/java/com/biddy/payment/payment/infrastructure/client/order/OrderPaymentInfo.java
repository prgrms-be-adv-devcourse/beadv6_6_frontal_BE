package com.biddy.payment.payment.infrastructure.client.order;

import java.time.LocalDateTime;

public record OrderPaymentInfo(
        Long orderId,
        Long buyerId,
        Long sellerId,
        Long amount,
        OrderPaymentStatus status,
        LocalDateTime paymentDueAt
) {

    public boolean isPaymentPending() {
        return status == OrderPaymentStatus.PAYMENT_PENDING;
    }

    public boolean isExpired(LocalDateTime now) {
        return paymentDueAt != null && paymentDueAt.isBefore(now);
    }
}
