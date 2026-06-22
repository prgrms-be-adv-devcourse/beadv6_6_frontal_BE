package com.biddy.payment.payment.infrastructure.kafka.consumer;

import com.biddy.payment.payment.application.PaymentService;
import com.biddy.payment.payment.domain.event.OrderCancelledEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCancelledEventConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public OrderCancelledEventConsumer(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order.cancelled", groupId = "payment-service-payment")
    public void consume(String payload) {
        try {
            OrderCancelledEvent event = objectMapper.readValue(payload, OrderCancelledEvent.class);
            paymentService.cancelByOrderCancellation(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("OrderCancelledEvent payload를 읽을 수 없습니다.", exception);
        }
    }
}
