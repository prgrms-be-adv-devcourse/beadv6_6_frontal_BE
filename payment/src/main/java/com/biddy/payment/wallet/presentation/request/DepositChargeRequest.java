package com.biddy.payment.wallet.presentation.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DepositChargeRequest(
        @NotNull Long userId,
        @NotNull @Positive Long amount,
        String paymentKey
) {
}
