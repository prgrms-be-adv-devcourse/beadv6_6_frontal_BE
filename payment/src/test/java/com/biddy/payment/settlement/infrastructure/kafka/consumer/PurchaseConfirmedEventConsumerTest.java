package com.biddy.payment.settlement.infrastructure.kafka.consumer;

import static org.mockito.Mockito.verify;

import com.biddy.payment.settlement.application.SettlementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PurchaseConfirmedEventConsumerTest {

    private final SettlementService settlementService = Mockito.mock(SettlementService.class);
    private final PurchaseConfirmedEventConsumer consumer = new PurchaseConfirmedEventConsumer(
            settlementService,
            new ObjectMapper().findAndRegisterModules()
    );

    @Test
    void consume_completesSettlementByOrderId() {
        Long orderId = 400L;
        String payload = """
                {
                  "eventId": "%s",
                  "orderId": %d,
                  "confirmedAt": "%s"
                }
                """.formatted(UUID.randomUUID(), orderId, LocalDateTime.now());

        consumer.consume(payload);

        verify(settlementService).completeByOrderId(orderId);
    }
}
