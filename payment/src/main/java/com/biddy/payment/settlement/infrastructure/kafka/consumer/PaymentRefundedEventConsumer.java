package com.biddy.payment.settlement.infrastructure.kafka.consumer;

import com.biddy.payment.settlement.application.SettlementService;
import com.biddy.payment.payment.domain.event.PaymentRefundedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentRefundedEventConsumer {

    private final SettlementService settlementService;
    private final ObjectMapper objectMapper;

    public PaymentRefundedEventConsumer(SettlementService settlementService, ObjectMapper objectMapper) {
        this.settlementService = settlementService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment.refunded", groupId = "payment-service-settlement")
    public void consume(String payload) {
        try {
            PaymentRefundedEvent event = objectMapper.readValue(payload, PaymentRefundedEvent.class);
            settlementService.cancelPendingSettlement(event.orderId());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("PaymentRefundedEvent payload를 읽을 수 없습니다.", exception);
        }
    }
}
