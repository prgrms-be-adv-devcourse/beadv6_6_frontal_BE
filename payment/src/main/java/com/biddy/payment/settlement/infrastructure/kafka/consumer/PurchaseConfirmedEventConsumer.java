package com.biddy.payment.settlement.infrastructure.kafka.consumer;

import com.biddy.payment.settlement.application.SettlementService;
import com.biddy.payment.settlement.domain.event.PurchaseConfirmedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PurchaseConfirmedEventConsumer {

    private final SettlementService settlementService;
    private final ObjectMapper objectMapper;

    public PurchaseConfirmedEventConsumer(SettlementService settlementService, ObjectMapper objectMapper) {
        this.settlementService = settlementService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "purchase.confirmed", groupId = "payment-service-settlement")
    public void consume(String payload) {
        try {
            PurchaseConfirmedEvent event = objectMapper.readValue(payload, PurchaseConfirmedEvent.class);
            settlementService.completeByOrderId(event.orderId());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("PurchaseConfirmedEvent payload를 읽을 수 없습니다.", exception);
        }
    }
}
