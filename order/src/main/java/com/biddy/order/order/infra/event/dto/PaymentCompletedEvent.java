package com.biddy.order.order.infra.event.dto;

import java.time.LocalDateTime;

public record PaymentCompletedEvent(
        Long paymentId,
        Long orderId,
        Long buyerId,
        Long sellerId,
        Long amount,
        String paymentMethod,
        LocalDateTime occurredAt
) {
}
