package com.biddy.payment.wallet.presentation.response;

import com.biddy.payment.wallet.domain.DepositTransaction;
import com.biddy.payment.wallet.domain.DepositTransactionType;
import java.time.LocalDateTime;

public record DepositTransactionResponse(
        Long id,
        Long userId,
        DepositTransactionType type,
        Long amount,
        Long balanceAfter,
        String referenceType,
        String referenceId,
        String reason,
        LocalDateTime createdAt
) {

    public static DepositTransactionResponse from(DepositTransaction transaction) {
        return new DepositTransactionResponse(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getReferenceType(),
                transaction.getReferenceId(),
                transaction.getReason(),
                transaction.getCreatedAt()
        );
    }
}
