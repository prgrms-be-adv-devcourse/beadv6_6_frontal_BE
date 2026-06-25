package com.biddy.order.cart.presentation.dto.response;

import com.biddy.order.cart.application.dto.CartResult;

import java.time.LocalDateTime;

public record CartResponse(
    Long id,
    Long userId,
    Long productId,
    LocalDateTime createdAt
){
    public static CartResponse from(CartResult result){
        return new CartResponse(
                result.id(),
                result.userId(),
                result.productId(),
                result.createdAt()
        );
    }
}
