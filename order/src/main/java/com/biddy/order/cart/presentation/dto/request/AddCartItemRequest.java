package com.biddy.order.cart.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "장바구니 담기")
public record AddCartItemRequest(
        @Schema(description = "장바구니 ID")
        Long id,
        @Schema(description = "유저 ID")
        Long userId,
        @Schema(description = "상품 ID")
        UUID productId,
        @Schema(description = "담기 요청 시간")
        LocalDateTime createdAt
) {
}
