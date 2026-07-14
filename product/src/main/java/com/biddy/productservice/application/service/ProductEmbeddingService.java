package com.biddy.productservice.application.service;

import com.biddy.productservice.infra.persistence.ProductEmbeddingRepository;
import com.biddy.productservice.presentation.dto.ProductEmbeddingUpsertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductEmbeddingService {

    private final ProductEmbeddingRepository productEmbeddingRepository;

    public void upsertEmbedding(Long productId, ProductEmbeddingUpsertRequest request) {
        productEmbeddingRepository.upsert(productId, request.embedding(), request.sourceText(), request.category());
    }
}
