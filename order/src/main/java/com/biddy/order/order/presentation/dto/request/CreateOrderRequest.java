package com.biddy.order.order.presentation.dto.request;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        List<OrderInfoRequest> items // OrderItemRequest -> OrderInfoRequest
) {
    public record OrderInfoRequest(
            UUID productId,
            Long orderPrice,
            Integer quantity,
            Long sellerId
    ) {}
}
