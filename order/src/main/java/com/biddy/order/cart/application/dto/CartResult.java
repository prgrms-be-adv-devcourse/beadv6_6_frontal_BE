package com.biddy.order.cart.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CartResult(
        Long id,
        Long userId,
        UUID productId,
        String productName,    //  상품명
        BigDecimal price,      //  가격
        String status,         //  상품 상태
        Long sellerId,         //  판매자 ID
        LocalDateTime createdAt
) {
}
