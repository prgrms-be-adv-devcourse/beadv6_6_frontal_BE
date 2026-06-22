package com.biddy.payment.wallet.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.biddy.payment.wallet.domain.DepositAccount;
import com.biddy.payment.wallet.infrastructure.client.toss.TossPaymentClient;
import com.biddy.payment.wallet.infrastructure.persistence.DepositAccountRepository;
import com.biddy.payment.wallet.infrastructure.persistence.DepositTransactionRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DepositServiceTest {

    private final DepositAccountRepository accountRepository = Mockito.mock(DepositAccountRepository.class);
    private final DepositTransactionRepository transactionRepository = Mockito.mock(DepositTransactionRepository.class);
    private final TossPaymentClient tossPaymentClient = Mockito.mock(TossPaymentClient.class);
    private final DepositService depositService = new DepositService(
            accountRepository,
            transactionRepository,
            tossPaymentClient
    );

    @Test
    void openAccount_createsDepositAccountWhenAbsent() {
        Long userId = 1L;
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        depositService.openAccount(userId);

        verify(accountRepository).save(any(DepositAccount.class));
    }

    @Test
    void openAccount_ignoresWhenDepositAccountAlreadyExists() {
        Long userId = 1L;
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(DepositAccount.open(userId)));

        depositService.openAccount(userId);

        verify(accountRepository, never()).save(any(DepositAccount.class));
    }
}
