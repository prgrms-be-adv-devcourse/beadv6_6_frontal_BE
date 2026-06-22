package com.biddy.order.order.presentation.dto.response;

import com.biddy.order.order.application.dto.OrderResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        Long id,
        Long userId,
        String status,
        Long totalPrice, // BigDecimal -> Long
        List<OrderInfoResponse> orderInfos, // OrderItemResponse -> OrderInfoResponse
        LocalDateTime createdAt
) {
    public record OrderInfoResponse(
            Long id,
            Long orderPrice,
            Integer quantity,
            UUID productId,
            Long sellerId,
            LocalDateTime createdAt
    ) {}

    public static OrderResponse from(OrderResult result) {
        if (result == null) return null;
        List<OrderInfoResponse> items = result.orderInfos().stream()
                .map(info -> new OrderInfoResponse(
                        info.id(),
                        info.orderPrice(),
                        info.quantity(),
                        info.productId(),
                        info.sellerId(),
                        info.createdAt()
                ))
                .toList();

        return new OrderResponse(
                result.id(),
                result.userId(),
                result.status(),
                result.totalPrice(),
                items,
                result.createdAt()
        );
    }
}
