package com.biddy.payment.settlement.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record MonthlySettlementRequest(
        @NotBlank String settlementYearMonth,
        @NotNull @DecimalMin("0.0000") @DecimalMax("1.0000") BigDecimal commissionRate,
        @NotEmpty List<@Valid MonthlySettlementItemRequest> items
) {
}
