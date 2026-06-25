package com.biddy.productservice.domain.event;

public record StockDeductedEvent(
        Long orderId,
        Long productId,
        int quantity
) {}