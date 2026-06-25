package com.biddy.productservice.application.service;

import com.biddy.productservice.domain.model.Product;
import com.biddy.productservice.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductStockService {

    private final ProductRepository productRepository;

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