package com.biddy.payment.settlement.presentation.response;

import com.biddy.payment.settlement.domain.SettlementItem;
import java.time.LocalDateTime;

public record SettlementItemResponse(
        Long id,
        Long settlementId,
        Long orderId,
        Long saleAmount,
        Long commissionAmount,
        Long netAmount,
        LocalDateTime createdAt
) {

    public static SettlementItemResponse from(SettlementItem item) {
        return new SettlementItemResponse(
                item.getId(),
                item.getSettlementId(),
                item.getOrderId(),
                item.getSaleAmount(),
                item.getCommissionAmount(),
                item.getNetAmount(),
                item.getCreatedAt()
        );
    }
}
