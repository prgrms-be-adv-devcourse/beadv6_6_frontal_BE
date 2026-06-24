package com.biddy.productservice.presentation.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductInfoResponse(
        UUID productId,
        String name,
        BigDecimal price,
        String status,
        Long sellerId,
        int stock
) {
}