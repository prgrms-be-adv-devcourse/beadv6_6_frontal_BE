package com.biddy.productservice.application.service;

import com.biddy.productservice.domain.model.Product;
import com.biddy.productservice.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductStatusService {

    private final ProductRepository productRepository;

    // 탈퇴 회원의 상품들을 비공개로 전환
    public void hideProductsBySeller(Long memberId) {
        List<Product> products = productRepository.findBySellerId(memberId);
        for (Product product : products) {
            product.changeStatus("HIDDEN");
        }
        productRepository.saveAll(products);
    }
}