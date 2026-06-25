package com.biddy.order.cart.application.dto;

import java.time.LocalDateTime;
public record CartInfoCommand(
        Long id,
        Long userId,
        Long productId,
        LocalDateTime createdAt
) {
}
