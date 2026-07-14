package com.biddy.recommendation.infra.acl;

import com.biddy.recommendation.application.service.ProductEmbeddingService;
import com.biddy.recommendation.application.service.ProductMatchNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductRegisteredEventListener {

    private final ProductEmbeddingService productEmbeddingService;
    private final ProductMatchNotificationService productMatchNotificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "product.registered", groupId = "recommendation-service")
    public void handleProductRegistered(String message) {
        try {
            ProductRegisteredEvent event = objectMapper.readValue(message, ProductRegisteredEvent.class);
            log.info("상품 등록 이벤트 수신: {}", message);
            productEmbeddingService.ensureEmbedding(event.productId());
            productMatchNotificationService.checkAndNotify(event.productId());
        } catch (Exception e) {
            log.error("상품 등록 이벤트 처리 실패: {}", message, e);
        }
    }
}
