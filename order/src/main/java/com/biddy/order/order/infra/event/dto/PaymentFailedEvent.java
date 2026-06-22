package com.biddy.order.order.infra.event.dto;

import java.time.LocalDateTime;

public record PaymentFailedEvent(
        Long paymentId,
        Long orderId,
        Long buyerId,
        Long amount,
        String reason,
        LocalDateTime occurredAt
) {
}
