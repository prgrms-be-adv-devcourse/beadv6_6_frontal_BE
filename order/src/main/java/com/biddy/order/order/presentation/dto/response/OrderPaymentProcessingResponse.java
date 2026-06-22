package com.biddy.order.order.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record OrderPaymentProcessingResponse(
        Long orderId,
        String status,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {}
