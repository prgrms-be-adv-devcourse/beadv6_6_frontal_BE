package com.biddy.payment.wallet.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DepositChargeRequest(
        @Schema(hidden = true) Long userId,
        @NotNull @Positive Long amount,
        @NotBlank String paymentKey,
        @NotBlank String orderId
) {
    public DepositChargeRequest withUserId(Long userId) {
        return new DepositChargeRequest(userId, amount, paymentKey, orderId);
    }
}
