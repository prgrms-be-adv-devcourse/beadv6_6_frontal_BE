package com.biddy.payment.payment.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentRefundedEvent(
        UUID eventId,
        Long paymentId,
        Long orderId,
        Long buyerId,
        Long amount,
        String reason,
        LocalDateTime refundedAt
) {
}
