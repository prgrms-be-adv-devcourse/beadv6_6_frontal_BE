package com.biddy.productservice.infra.event;

import com.biddy.productservice.domain.event.ProductRegisteredForAuctionEvent;
import com.biddy.productservice.domain.event.StockDeductFailedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventProducer {

    private static final String TOPIC = "product.auction.registered";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendStockDeductFailed(StockDeductFailedEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("order.stock.deduct.failed", message);
            log.info("재고차감 실패 이벤트 발행: {}", message);
        } catch (Exception e) {
            log.error("재고차감 실패 이벤트 발행 실패", e);
        }
    }

    public void sendAuctionRegistered(ProductRegisteredForAuctionEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, message);
            log.info("경매 등록 이벤트 발행: {}", message);
        } catch (Exception e) {
            log.error("경매 등록 이벤트 발행 실패", e);
        }
    }
}