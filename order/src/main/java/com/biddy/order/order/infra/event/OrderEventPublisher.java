package com.biddy.order.order.infra.event;

import com.biddy.order.order.application.dto.event.CancelPaymentEvent;
import com.biddy.order.order.application.dto.event.DecreaseStockEvent;
import com.biddy.order.order.application.dto.event.RequestSettlementEvent;
import com.biddy.order.order.application.dto.event.RestoreStockEvent;
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

    // 제네릭 Object 타입의 KafkaTemplate을 주입받아 다양한 이벤트 전송
    private final KafkaTemplate<String, Object> orderKafkaTemplate;

    // 1. 결제취소 이벤트 발행 (수신처: payment)
    public void publishCancelPayment(Long orderId, Long userId) {
        String topic = "payment-service";
        CancelPaymentEvent event = new CancelPaymentEvent(orderId, userId);
        
        orderKafkaTemplate.send(topic, String.valueOf(orderId), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish CancelPaymentEvent for orderId: {}", orderId, throwable);
                    } else {
                        log.info("Successfully published CancelPaymentEvent to topic {}: {}", topic, event);
                    }
                });
    }

    // 2. 정산요청 이벤트 발행 (수신처: settlement)
    public void publishRequestSettlement(Long orderId, Long userId) {
        String topic = "settlement-service";
        RequestSettlementEvent event = new RequestSettlementEvent(orderId, userId);
        
        orderKafkaTemplate.send(topic, String.valueOf(orderId), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish RequestSettlementEvent for orderId: {}", orderId, throwable);
                    } else {
                        log.info("Successfully published RequestSettlementEvent to topic {}: {}", topic, event);
                    }
                });
    }

    // 3. 재고차감 이벤트 발행 (수신처: product)
    public void publishDecreaseStock(UUID productId, Integer quantity) {
        String topic = "product-service";
        DecreaseStockEvent event = new DecreaseStockEvent(productId, quantity);
        
        orderKafkaTemplate.send(topic, productId.toString(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish DecreaseStockEvent for productId: {}", productId, throwable);
                    } else {
                        log.info("Successfully published DecreaseStockEvent to topic {}: {}", topic, event);
                    }
                });
    }

    // 4. 재고원복 이벤트 발행 (수신처: product)
    public void publishRestoreStock(UUID productId, Integer quantity) {
        String topic = "product-service";
        RestoreStockEvent event = new RestoreStockEvent(productId, quantity);
        
        orderKafkaTemplate.send(topic, productId.toString(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish RestoreStockEvent for productId: {}", productId, throwable);
                    } else {
                        log.info("Successfully published RestoreStockEvent to topic {}: {}", topic, event);
                    }
                });
    }
}
