package com.biddy.payment.settlement.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PurchaseConfirmedEvent(
        UUID eventId,
        Long orderId,
        LocalDateTime confirmedAt
) {
}
