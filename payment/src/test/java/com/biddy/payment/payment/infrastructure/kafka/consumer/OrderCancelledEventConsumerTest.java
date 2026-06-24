package com.biddy.payment.payment.infrastructure.kafka.consumer;

import static org.mockito.Mockito.verify;

import com.biddy.payment.payment.application.PaymentService;
import com.biddy.payment.payment.domain.event.OrderCancelledEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class OrderCancelledEventConsumerTest {

    private final PaymentService paymentService = Mockito.mock(PaymentService.class);
    private final OrderCancelledEventConsumer consumer = new OrderCancelledEventConsumer(
            paymentService,
            new ObjectMapper().findAndRegisterModules()
    );

    @Test
    void consume_cancelsPaymentByOrderCancellation() {
        UUID eventId = UUID.randomUUID();
        Long orderId = 100L;
        Long userId = 1L;
        String payload = """
                {
                  "eventId": "%s",
                  "orderId": %d,
                  "userId": %d,
                  "reason": "주문 취소",
                  "cancelledAt": "%s"
                }
                """.formatted(eventId, orderId, userId, LocalDateTime.now());

        consumer.consume(payload);

        ArgumentCaptor<OrderCancelledEvent> eventCaptor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        verify(paymentService).cancelByOrderCancellation(eventCaptor.capture());
        OrderCancelledEvent event = eventCaptor.getValue();

        org.assertj.core.api.Assertions.assertThat(event.eventId()).isEqualTo(eventId);
        org.assertj.core.api.Assertions.assertThat(event.orderId()).isEqualTo(orderId);
        org.assertj.core.api.Assertions.assertThat(event.userId()).isEqualTo(userId);
    }
}
