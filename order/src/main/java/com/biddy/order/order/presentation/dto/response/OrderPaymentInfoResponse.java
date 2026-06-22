package com.biddy.order.order.presentation.dto.response;

import java.time.LocalDateTime;

public record OrderPaymentInfoResponse(
        Long orderId,
        Long buyerId,
        Long sellerId,
        Long amount,
        String status,
        LocalDateTime paymentDueAt
) {}
