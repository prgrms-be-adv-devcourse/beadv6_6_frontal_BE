package com.biddy.order.cart.presentation.dto.response;

import com.biddy.order.cart.application.dto.CartResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record CartResponse(
    Long id,
    Long userId,
    UUID productId,
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
