package com.biddy.payment.payment.domain.event;

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
