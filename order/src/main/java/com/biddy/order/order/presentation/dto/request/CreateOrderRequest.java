package com.biddy.order.order.presentation.dto.request;

import java.util.List;

public record CreateOrderRequest(
        List<OrderInfoRequest> items // OrderItemRequest -> OrderInfoRequest
) {
    public record OrderInfoRequest(
            Long productId,
            Long orderPrice,
            Integer quantity,
            Long sellerId
    ) {}
}
