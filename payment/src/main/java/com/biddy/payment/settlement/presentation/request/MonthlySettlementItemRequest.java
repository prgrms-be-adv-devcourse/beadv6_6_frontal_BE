package com.biddy.payment.settlement.presentation.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MonthlySettlementItemRequest(
        @NotNull Long sellerId,
        @NotNull Long orderId,
        @NotNull @Positive Long saleAmount
) {
}
