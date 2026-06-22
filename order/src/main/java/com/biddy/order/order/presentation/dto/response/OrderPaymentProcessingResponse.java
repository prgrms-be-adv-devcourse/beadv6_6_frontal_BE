package com.biddy.order.order.presentation.dto.response;

import java.time.LocalDateTime;

public record OrderPaymentProcessingResponse(
        Long orderId,
        String status,
        LocalDateTime paymentProcessingStartedAt
) {}
