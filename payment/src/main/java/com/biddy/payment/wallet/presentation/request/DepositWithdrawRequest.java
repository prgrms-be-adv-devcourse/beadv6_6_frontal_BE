package com.biddy.payment.wallet.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DepositWithdrawRequest(
        @Schema(hidden = true) Long userId,
        @NotNull @Positive Long amount,
        @NotBlank String bankName,
        @NotBlank String accountNumber,
        @NotBlank String holderName
) {
    public DepositWithdrawRequest withUserId(Long userId) {
        return new DepositWithdrawRequest(userId, amount, bankName, accountNumber, holderName);
    }
}
