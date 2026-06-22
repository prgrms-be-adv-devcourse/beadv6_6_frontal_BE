package com.biddy.order.order.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record OrderPaymentInfoResponse(
        Long orderId,
        @JsonProperty("user_id") Long userId,
        Long buyerId,
        Long sellerId,
        @JsonProperty("total_price") Long totalPrice,
        Long amount,
        String status,
        @JsonProperty("updated_at") LocalDateTime updatedAt,
        LocalDateTime paymentDueAt
) {
    public OrderPaymentInfoResponse(Long orderId, Long userId, Long totalPrice, String status, LocalDateTime updatedAt) {
        this(
                orderId,
                userId,
                userId,      // buyerId
                null,        // sellerId
                totalPrice,
                totalPrice,  // amount
                status,
                updatedAt,
                updatedAt != null ? updatedAt.plusDays(1) : null // paymentDueAt
        );
    }
}
