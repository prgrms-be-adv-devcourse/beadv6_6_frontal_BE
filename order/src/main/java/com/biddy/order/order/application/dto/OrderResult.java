package com.biddy.order.order.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResult(
        Long id,
        Long userId,
        String status,
        Long totalPrice, // BigDecimal -> Long
        List<OrderInfoResult> orderInfos, // OrderItemResult -> OrderInfoResult
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record OrderInfoResult(
            Long id,
            Long orderPrice, // BigDecimal -> Long
            Integer quantity, // 수량 추가
            UUID productId,
            LocalDateTime createdAt
    ) {}
}
