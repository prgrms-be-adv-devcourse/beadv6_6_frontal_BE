package com.biddy.order.cart.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CartInfoCommand(
        Long id,
        Long userId,
        UUID productId,
        LocalDateTime createdAt
) {
}
