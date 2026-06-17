package com.biddy.payment.wallet.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DepositWithdrawRequest(
        @NotNull Long userId,
        @NotNull @Positive Long amount,
        @NotBlank String bankName,
        @NotBlank String accountNumber,
        @NotBlank String holderName
) {
}
