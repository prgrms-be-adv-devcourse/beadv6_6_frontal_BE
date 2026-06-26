package com.biddy.productservice.application.service;

import com.biddy.productservice.domain.model.ProcessedOrder;
import com.biddy.productservice.domain.model.Product;
import com.biddy.productservice.domain.repository.ProductRepository;
import com.biddy.productservice.infra.persistence.ProcessedOrderJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductStockService {

    private final ProductRepository productRepository;
    private final ProcessedOrderJpaRepository processedOrderRepository;

    // 멱등성 체크 + 재고차감을 단일 트랜잭션으로 처리
    public void deductStockIdempotently(Long productId, int quantity, Long orderId) {
        if (processedOrderRepository.existsByOrderId(orderId)) {
            log.warn("중복 재고차감 이벤트 무시 - orderId: {}", orderId);
            return;
        }
        deductStock(productId, quantity);
        processedOrderRepository.save(new ProcessedOrder(orderId));
    }

    public void deductStock(Long productId, int quantity) {
        Product product = findProduct(productId);
        product.deductStock(quantity);
        if (product.getStock() == 0) {
            product.changeStatus("SOLD_OUT");
        }
        productRepository.save(product);
    }

    public void restoreStock(Long productId, int quantity) {
        Product product = findProduct(productId);
        product.restoreStock(quantity);
        productRepository.save(product);
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }
}