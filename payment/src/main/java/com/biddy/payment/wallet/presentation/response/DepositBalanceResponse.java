package com.biddy.payment.wallet.presentation.response;

import com.biddy.payment.wallet.domain.DepositAccount;
import java.time.LocalDateTime;

public record DepositBalanceResponse(
        Long userId,
        Long balance,
        LocalDateTime updatedAt
) {

    public static DepositBalanceResponse from(DepositAccount account) {
        return new DepositBalanceResponse(account.getUserId(), account.getBalance(), account.getUpdatedAt());
    }
}
