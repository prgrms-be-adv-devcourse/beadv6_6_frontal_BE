package com.biddy.payment.settlement.presentation.response;

import com.biddy.payment.settlement.domain.Settlement;
import com.biddy.payment.settlement.domain.SettlementStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SettlementResponse(
        Long id,
        Long userId,
        String settlementYearMonth,
        Long totalAmount,
        BigDecimal commissionRate,
        Long commissionAmount,
        Long settlementAmount,
        SettlementStatus status,
        LocalDateTime settledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<SettlementItemResponse> items
) {

    public static SettlementResponse from(Settlement settlement, List<SettlementItemResponse> items) {
        return new SettlementResponse(
                settlement.getId(),
                settlement.getUserId(),
                settlement.getSettlementYearMonth(),
                settlement.getTotalAmount(),
                settlement.getCommissionRate(),
                settlement.getCommissionAmount(),
                settlement.getSettlementAmount(),
                settlement.getStatus(),
                settlement.getSettledAt(),
                settlement.getCreatedAt(),
                settlement.getUpdatedAt(),
                items
        );
    }
}
