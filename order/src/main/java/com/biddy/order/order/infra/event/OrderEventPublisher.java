package com.biddy.order.order.infra.event;

import com.biddy.order.order.application.dto.event.CancelPaymentEvent;
import com.biddy.order.order.application.dto.event.DecreaseStockEvent;
import com.biddy.order.order.application.dto.event.RequestSettlementEvent;
import com.biddy.order.order.application.dto.event.RestoreStockEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
public class OrderEventPublisher {

    private final KafkaTemplate<String, String> orderKafkaTemplate;
    private final ObjectMapper objectMapper;

    // 1. 결제취소 이벤트 발행 (수신처: payment)
    public void publishCancelPayment(Long orderId, Long userId) {
        String topic = "payment-service";
        CancelPaymentEvent event = new CancelPaymentEvent(orderId, userId);
        send(topic, String.valueOf(orderId), event);
    }

    // 2. 정산요청 이벤트 발행 (수신처: settlement)
    public void publishRequestSettlement(Long orderId, Long userId) {
        String topic = "settlement-service";
        RequestSettlementEvent event = new RequestSettlementEvent(orderId, userId);
        send(topic, String.valueOf(orderId), event);
    }

    // 3. 재고차감 이벤트 발행 (수신처: product)
    public void publishDecreaseStock(UUID productId, Integer quantity) {
        String topic = "product-service";
        DecreaseStockEvent event = new DecreaseStockEvent(productId, quantity);
        send(topic, productId.toString(), event);
    }

    // 4. 재고원복 이벤트 발행 (수신처: product)
    public void publishRestoreStock(UUID productId, Integer quantity) {
        String topic = "product-service";
        RestoreStockEvent event = new RestoreStockEvent(productId, quantity);
        send(topic, productId.toString(), event);
    }

    private void send(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            orderKafkaTemplate.send(topic, key, payload)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("Failed to publish event to topic {} for key: {}", topic, key, throwable);
                        } else {
                            log.info("Successfully published event to topic {}: {}", topic, payload);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Kafka event for topic: {}", topic, e);
        }
    }
}
