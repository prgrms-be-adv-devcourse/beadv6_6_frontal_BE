package com.biddy.order.order.application.dto.event;

import java.util.UUID;

public record DecreaseStockEvent(
        UUID orderId,
        UUID productId,
        Integer quantity
) {
}
