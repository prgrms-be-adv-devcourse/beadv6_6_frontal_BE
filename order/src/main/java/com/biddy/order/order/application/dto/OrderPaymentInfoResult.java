package com.biddy.order.order.application.dto;

import java.time.LocalDateTime;

public record OrderPaymentInfoResult(
        Long orderId,
        Long userId,
        Long totalPrice,
        String status,
        LocalDateTime updatedAt
) {}
