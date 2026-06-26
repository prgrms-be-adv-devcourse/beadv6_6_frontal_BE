package com.biddy.productservice.infra.event;

import com.biddy.productservice.application.service.ProductStatusService;
import com.biddy.productservice.application.service.ProductStockService;
import com.biddy.productservice.domain.event.MemberWithdrawnEvent;
import com.biddy.productservice.domain.event.StockDeductedEvent;
import com.biddy.productservice.domain.event.StockRestoredEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final ProductStockService stockService;
    private final ProductStatusService statusService;
    private final ObjectMapper objectMapper;

    // 재고차감 수신 (멱등성 + 동시성은 서비스 레이어에서 단일 트랜잭션으로 처리)
    @KafkaListener(topics = "order.stock.deduct", groupId = "product-service")
    public void handleStockDeduct(String message) {
        try {
            StockDeductedEvent event = objectMapper.readValue(message, StockDeductedEvent.class);
            log.info("재고차감 이벤트 수신: {}", message);
            stockService.deductStockIdempotently(event.productId(), event.quantity(), event.orderId());
        } catch (OptimisticLockingFailureException e) {
            log.error("재고차감 동시성 충돌 - 재시도 필요: {}", message);
            throw e; // Kafka가 재시도 처리
        } catch (Exception e) {
            log.error("재고차감 처리 실패", e);
        }
    }

    // 재고원복 수신 (보상트랜잭션)
    @KafkaListener(topics = "order.stock.restore", groupId = "product-service")
    public void handleStockRestore(String message) {
        try {
            StockRestoredEvent event = objectMapper.readValue(message, StockRestoredEvent.class);
            log.info("재고원복(보상트랜잭션) 이벤트 수신: {}", message);
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