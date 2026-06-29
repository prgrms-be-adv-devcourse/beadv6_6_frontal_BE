package com.biddy.productservice.domain.event;

public record StockDeductFailedEvent(
        Long orderId,
        Long productId,
        String reason
) {}
