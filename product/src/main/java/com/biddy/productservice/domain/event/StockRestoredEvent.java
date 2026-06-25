package com.biddy.productservice.domain.event;

public record StockRestoredEvent(
        Long orderId,
        Long productId,
        int quantity
) {}