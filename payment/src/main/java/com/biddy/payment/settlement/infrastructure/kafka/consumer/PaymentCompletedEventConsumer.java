package com.biddy.payment.settlement.infrastructure.kafka.consumer;

import com.biddy.payment.settlement.application.SettlementService;
import com.biddy.payment.payment.domain.event.PaymentCompletedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentCompletedEventConsumer {

    private final SettlementService settlementService;
    private final ObjectMapper objectMapper;

    public PaymentCompletedEventConsumer(SettlementService settlementService, ObjectMapper objectMapper) {
        this.settlementService = settlementService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment.completed", groupId = "payment-service-settlement")
    public void consume(String payload) {
        try {
            PaymentCompletedEvent event = objectMapper.readValue(payload, PaymentCompletedEvent.class);
            settlementService.createPendingSettlement(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("PaymentCompletedEvent payload를 읽을 수 없습니다.", exception);
        }
    }
}
