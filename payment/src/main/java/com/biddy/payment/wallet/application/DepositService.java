package com.biddy.payment.wallet.application;

import com.biddy.payment.wallet.domain.DepositAccount;
import com.biddy.payment.wallet.domain.DepositTransaction;
import com.biddy.payment.wallet.domain.DepositTransactionType;
import com.biddy.payment.wallet.presentation.request.DepositAdjustRequest;
import com.biddy.payment.wallet.presentation.response.DepositBalanceResponse;
import com.biddy.payment.wallet.presentation.request.DepositChargeRequest;
import com.biddy.payment.wallet.presentation.response.DepositTransactionResponse;
import com.biddy.payment.wallet.presentation.request.DepositWithdrawRequest;
import com.biddy.payment.wallet.infrastructure.persistence.DepositAccountRepository;
import com.biddy.payment.wallet.infrastructure.persistence.DepositTransactionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepositService {

    private final DepositAccountRepository accountRepository;
    private final DepositTransactionRepository transactionRepository;

    public DepositService(
            DepositAccountRepository accountRepository,
            DepositTransactionRepository transactionRepository
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public DepositBalanceResponse getBalance(Long userId) {
        DepositAccount account = accountRepository.findByUserId(userId)
                .orElseGet(() -> DepositAccount.open(userId));
        return DepositBalanceResponse.from(account);
    }

    @Transactional(readOnly = true)
    public List<DepositTransactionResponse> getTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(DepositTransactionResponse::from)
                .toList();
    }

    @Transactional
    public DepositBalanceResponse charge(DepositChargeRequest request) {
        DepositAccount account = getOrCreateWithLock(request.userId());
        account.increase(request.amount());
        record(account, DepositTransactionType.CHARGE, request.amount(), "DEPOSIT_CHARGE", request.paymentKey(), "예치금 충전");
        return DepositBalanceResponse.from(account);
    }

    @Transactional
    public DepositBalanceResponse withdraw(DepositWithdrawRequest request) {
        DepositAccount account = getOrCreateWithLock(request.userId());
        account.decrease(request.amount());
        record(account, DepositTransactionType.WITHDRAW, -request.amount(), "DEPOSIT_WITHDRAW", null, "예치금 출금 신청");
        return DepositBalanceResponse.from(account);
    }

    @Transactional
    public DepositBalanceResponse adjust(DepositAdjustRequest request) {
        if (request.amount() == 0) {
            throw new IllegalArgumentException("조정 금액은 0일 수 없습니다.");
        }
        DepositAccount account = getOrCreateWithLock(request.userId());
        account.adjust(request.amount());
        record(account, DepositTransactionType.ADJUSTMENT, request.amount(), "DEPOSIT_ADJUST", null, request.reason());
        return DepositBalanceResponse.from(account);
    }

    @Transactional
    public void decreaseForPayment(Long userId, Long amount, String referenceId) {
        DepositAccount account = getOrCreateWithLock(userId);
        account.decrease(amount);
        record(account, DepositTransactionType.PAYMENT, -amount, "PAYMENT", referenceId, "예치금 결제 차감");
    }

    @Transactional
    public void increaseForCancel(Long userId, Long amount, String referenceId, DepositTransactionType type, String reason) {
        DepositAccount account = getOrCreateWithLock(userId);
        account.increase(amount);
        record(account, type, amount, "PAYMENT", referenceId, reason);
    }

    @Transactional
    public void increaseForSettlement(Long userId, Long amount, String referenceId) {
        DepositAccount account = getOrCreateWithLock(userId);
        account.increase(amount);
        record(account, DepositTransactionType.SETTLEMENT, amount, "SETTLEMENT", referenceId, "판매자 정산 지급");
    }

    private DepositAccount getOrCreateWithLock(Long userId) {
        return accountRepository.findWithLockByUserId(userId)
                .orElseGet(() -> accountRepository.save(DepositAccount.open(userId)));
    }

    private void record(
            DepositAccount account,
            DepositTransactionType type,
            Long amount,
            String referenceType,
            String referenceId,
            String reason
    ) {
        transactionRepository.save(DepositTransaction.record(
                account.getUserId(),
                type,
                amount,
                account.getBalance(),
                referenceType,
                referenceId,
                reason
        ));
    }
}
