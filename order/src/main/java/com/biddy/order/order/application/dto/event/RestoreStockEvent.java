package com.biddy.order.order.application.dto.event;

import java.util.UUID;

public record RestoreStockEvent(
        UUID productId,
        Integer quantity
) {
}
