package com.biddy.payment.payment.presentation.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentCancelRequest(
        @NotNull @Positive Long amount,
        String reason
) {
}
