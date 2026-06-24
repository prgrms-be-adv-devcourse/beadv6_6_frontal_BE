package com.biddy.order.order.application.dto.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PurchaseConfirmedEvent(
        UUID eventId,
        Long orderId,
        LocalDateTime confirmedAt
) {
}
