package com.biddy.order.order.application.dto.event;

import java.util.UUID;

public record RestoreStockEvent(
        UUID orderId,
        UUID productId,
        Integer quantity
) {
}
