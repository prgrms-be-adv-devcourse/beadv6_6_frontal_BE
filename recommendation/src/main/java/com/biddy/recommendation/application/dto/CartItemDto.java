package com.biddy.recommendation.application.dto;

import java.time.LocalDateTime;

public record CartItemDto(
        Long id,
        Long userId,
        Long productId,
        LocalDateTime createdAt
) {
}
