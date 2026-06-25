package com.biddy.order.cart.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
public record CartResult(
        Long id,
        Long userId,
        Long productId,
        String productName,    //  상품명
        BigDecimal price,      //  가격
        String status,         //  상품 상태
        Long sellerId,         //  판매자 ID
        LocalDateTime createdAt
) {
}
