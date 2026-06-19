package com.biddy.payment.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.biddy.payment.payment.domain.PaymentMethod;
import com.biddy.payment.payment.domain.PaymentStatus;
import com.biddy.payment.payment.domain.event.PaymentCompletedEvent;
import com.biddy.payment.payment.infrastructure.client.order.OrderClient;
import com.biddy.payment.payment.infrastructure.client.order.OrderPaymentInfo;
import com.biddy.payment.payment.infrastructure.client.order.OrderPaymentStatus;
import com.biddy.payment.payment.infrastructure.kafka.producer.PaymentEventProducer;
import com.biddy.payment.payment.presentation.request.PaymentCreateRequest;
import com.biddy.payment.payment.presentation.response.PaymentResponse;
import com.biddy.payment.wallet.application.DepositService;
import com.biddy.payment.wallet.presentation.request.DepositAdjustRequest;
import com.biddy.payment.wallet.presentation.response.DepositBalanceResponse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private DepositService depositService;

    @MockBean
    private OrderClient orderClient;

    @MockBean
    private PaymentEventProducer paymentEventProducer;

    @Test
    void walletPayment_decreasesDepositAndPublishesCompletedEvent() {
        Long orderId = 100L;
        Long buyerId = 1L;
        Long sellerId = 2L;
        Long amount = 50_000L;

        depositService.adjust(new DepositAdjustRequest(buyerId, 100_000L, "테스트 예치금 지급"));
        when(orderClient.getPaymentInfo(orderId)).thenReturn(new OrderPaymentInfo(
                orderId,
                buyerId,
                sellerId,
                amount,
                OrderPaymentStatus.PAYMENT_PENDING,
                LocalDateTime.now().plusMinutes(10)
        ));

        PaymentResponse response = paymentService.create(new PaymentCreateRequest(
                orderId,
                buyerId,
                amount,
                PaymentMethod.WALLET,
                null
        ));

        assertThat(response.status()).isEqualTo(PaymentStatus.COMPLETED);
        DepositBalanceResponse balance = depositService.getBalance(buyerId);
        assertThat(balance.balance()).isEqualTo(50_000L);

        verify(orderClient).requestPaymentProcessing(eq(orderId), eq(buyerId));

        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(paymentEventProducer).publish(eventCaptor.capture());
        PaymentCompletedEvent event = eventCaptor.getValue();

        assertThat(event.eventId()).isNotNull();
        assertThat(event.paymentId()).isEqualTo(response.id());
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.buyerId()).isEqualTo(buyerId);
        assertThat(event.sellerId()).isEqualTo(sellerId);
        assertThat(event.amount()).isEqualTo(amount);
        assertThat(event.paymentMethod()).isEqualTo(PaymentMethod.WALLET);
        assertThat(event.paidAt()).isNotNull();
    }
}
