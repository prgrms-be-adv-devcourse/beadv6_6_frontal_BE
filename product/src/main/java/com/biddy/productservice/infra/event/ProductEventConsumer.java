package com.biddy.productservice.infra.event;

import com.biddy.productservice.application.service.ProductStatusService;
import com.biddy.productservice.application.service.ProductStockService;
import com.biddy.productservice.domain.event.MemberWithdrawnEvent;
import com.biddy.productservice.domain.event.StockDeductedEvent;
import com.biddy.productservice.domain.event.StockRestoredEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final ProductStockService stockService;
    private final ProductStatusService statusService;
    private final ObjectMapper objectMapper;

    // 재고차감 수신
    @KafkaListener(topics = "order.stock.deduct", groupId = "product-service")
    public void handleStockDeduct(String message) {
        try {
            StockDeductedEvent event = objectMapper.readValue(message, StockDeductedEvent.class);
            log.info("재고차감 이벤트 수신: {}", message);
            stockService.deductStock(event.productId(), event.quantity());
        } catch (Exception e) {
            log.error("재고차감 처리 실패", e);
        }
    }

    // 재고원복 수신
    @KafkaListener(topics = "order.stock.restore", groupId = "product-service")
    public void handleStockRestore(String message) {
        try {
            StockRestoredEvent event = objectMapper.readValue(message, StockRestoredEvent.class);
            log.info("재고원복 이벤트 수신: {}", message);
            stockService.restoreStock(event.productId(), event.quantity());
        } catch (Exception e) {
            log.error("재고원복 처리 실패", e);
        }
    }

    // 회원탈퇴 수신
    @KafkaListener(topics = "member-withdraw", groupId = "product-service")
    public void handleMemberWithdraw(String message) {
        try {
            MemberWithdrawnEvent event = objectMapper.readValue(message, MemberWithdrawnEvent.class);
            log.info("회원탈퇴 이벤트 수신: {}", message);
            statusService.hideProductsBySeller(event.memberId());
        } catch (Exception e) {
            log.error("회원탈퇴 처리 실패", e);
        }
    }
}