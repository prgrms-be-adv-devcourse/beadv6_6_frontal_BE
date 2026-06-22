package com.biddy.payment.payment.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID eventId,
        Long orderId,
        Long userId,
        String reason,
        LocalDateTime cancelledAt
) {
}
