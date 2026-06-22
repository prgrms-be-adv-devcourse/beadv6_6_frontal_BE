package com.biddy.payment.wallet.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DepositAdjustRequest(
        @NotNull Long userId,
        @NotNull Long amount,
        @NotBlank String reason
) {
}
