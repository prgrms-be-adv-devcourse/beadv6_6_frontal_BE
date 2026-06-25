package com.biddy.order.order.application.dto.event;

public record DecreaseStockEvent(
        Long orderId,
        Long productId,
        Integer quantity
) {
}
