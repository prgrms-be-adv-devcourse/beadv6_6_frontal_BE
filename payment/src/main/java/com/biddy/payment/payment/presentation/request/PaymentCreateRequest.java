package com.biddy.payment.payment.presentation.request;

import com.biddy.payment.payment.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentCreateRequest(
        @NotNull Long orderId,
        @NotNull Long userId,
        @NotNull @Positive Long amount,
        @NotNull PaymentMethod paymentMethod,
        String pgTransactionId,
        String paymentKey,
        String tossOrderId
) {
}
