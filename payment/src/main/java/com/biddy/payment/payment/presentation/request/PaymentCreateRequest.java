package com.biddy.payment.payment.presentation.request;

import com.biddy.payment.payment.domain.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentCreateRequest(
        @NotNull Long orderId,
        @Schema(hidden = true) Long userId,
        @NotNull @Positive Long amount,
        @NotNull PaymentMethod paymentMethod,
        String pgTransactionId,
        String paymentKey,
        String tossOrderId
) {
    public PaymentCreateRequest withUserId(Long userId) {
        return new PaymentCreateRequest(
                orderId,
                userId,
                amount,
                paymentMethod,
                pgTransactionId,
                paymentKey,
                tossOrderId
        );
    }
}
