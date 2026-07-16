package com.biddy.payment.settlement.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.biddy.payment.payment.domain.PaymentMethod;
import com.biddy.payment.payment.domain.event.PaymentCompletedEvent;
import com.biddy.payment.settlement.domain.SettlementStatus;
import com.biddy.payment.settlement.infrastructure.persistence.SettlementRepository;
import com.biddy.payment.wallet.application.DepositService;
import com.biddy.payment.wallet.presentation.response.DepositBalanceResponse;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SettlementServiceTest {

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private DepositService depositService;

    @Test
    void completeByOrderId_completesPendingSettlementAndIncreasesSellerDeposit() {
        Long orderId = 300L;
        Long sellerId = 30L;
        Long amount = 100_000L;

        settlementService.createPendingSettlement(new PaymentCompletedEvent(
                UUID.randomUUID(),
                1L,
                orderId,
                10L,
                sellerId,
                amount,
                PaymentMethod.WALLET,
                LocalDateTime.now()
        ));

        settlementService.completeByOrderId(orderId);

        var settlement = settlementRepository.findByOrderId(orderId).orElseThrow();
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(settlement.getCommissionAmount()).isEqualTo(5_000L);
        assertThat(settlement.getSettlementAmount()).isEqualTo(95_000L);

        DepositBalanceResponse balance = depositService.getBalance(sellerId);
        assertThat(balance.balance()).isEqualTo(95_000L);
    }

    @Test
    void completeByOrderId_ignoresAlreadyCompletedSettlement() {
        Long orderId = 301L;
        Long sellerId = 31L;
        Long amount = 80_000L;

        settlementService.createPendingSettlement(new PaymentCompletedEvent(
                UUID.randomUUID(),
                2L,
                orderId,
                11L,
                sellerId,
                amount,
                PaymentMethod.NORMAL,
                LocalDateTime.now()
        ));

        settlementService.completeByOrderId(orderId);
        settlementService.completeByOrderId(orderId);

        DepositBalanceResponse balance = depositService.getBalance(sellerId);
        assertThat(balance.balance()).isEqualTo(76_000L);
    }

    @Test
    void markReadyByOrderId_marksPendingSettlementReadyWithoutIncreasingSellerDeposit() {
        Long orderId = 302L;
        Long sellerId = 32L;
        Long amount = 50_000L;

        settlementService.createPendingSettlement(new PaymentCompletedEvent(
                UUID.randomUUID(),
                3L,
                orderId,
                12L,
                sellerId,
                amount,
                PaymentMethod.NORMAL,
                LocalDateTime.now()
        ));

        settlementService.markReadyByOrderId(orderId);

        var settlement = settlementRepository.findByOrderId(orderId).orElseThrow();
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.READY);

        DepositBalanceResponse balance = depositService.getBalance(sellerId);
        assertThat(balance.balance()).isZero();
    }

    @Test
    void completeReadySettlements_completesOnlyReadySettlementsAndIncreasesSellerDeposit() {
        Long readyOrderId = 303L;
        Long pendingOrderId = 304L;
        Long readySellerId = 33L;
        Long pendingSellerId = 34L;

        settlementService.createPendingSettlement(new PaymentCompletedEvent(
                UUID.randomUUID(),
                4L,
                readyOrderId,
                13L,
                readySellerId,
                120_000L,
                PaymentMethod.WALLET,
                LocalDateTime.now()
        ));
        settlementService.createPendingSettlement(new PaymentCompletedEvent(
                UUID.randomUUID(),
                5L,
                pendingOrderId,
                14L,
                pendingSellerId,
                70_000L,
                PaymentMethod.WALLET,
                LocalDateTime.now()
        ));
        settlementService.markReadyByOrderId(readyOrderId);

        int completedCount = settlementService.completeReadySettlements();

        var readySettlement = settlementRepository.findByOrderId(readyOrderId).orElseThrow();
        var pendingSettlement = settlementRepository.findByOrderId(pendingOrderId).orElseThrow();
        assertThat(completedCount).isEqualTo(1);
        assertThat(readySettlement.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(pendingSettlement.getStatus()).isEqualTo(SettlementStatus.PENDING);

        assertThat(depositService.getBalance(readySellerId).balance()).isEqualTo(114_000L);
        assertThat(depositService.getBalance(pendingSellerId).balance()).isZero();
    }
}
