package com.biddy.order.order.infra.event.dto;

public record StockDeductFailedEvent(
        Long orderId,
        Long productId,
        Integer quantity,
        String reason
) {
}
