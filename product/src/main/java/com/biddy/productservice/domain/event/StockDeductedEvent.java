package com.biddy.productservice.domain.event;

import java.util.UUID;

public record StockDeductedEvent(
        UUID orderId,
        UUID productId,
        int quantity
) {}