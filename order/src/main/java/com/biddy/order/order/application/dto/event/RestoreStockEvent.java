package com.biddy.order.order.application.dto.event;

public record RestoreStockEvent(
        Long orderId,
        Long productId,
        Integer quantity
) {
}
