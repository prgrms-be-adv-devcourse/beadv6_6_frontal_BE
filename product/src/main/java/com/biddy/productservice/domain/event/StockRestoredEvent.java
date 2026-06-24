package com.biddy.productservice.domain.event;

import java.util.UUID;

public record StockRestoredEvent(
        UUID orderId,
        UUID productId,
        int quantity
) {}