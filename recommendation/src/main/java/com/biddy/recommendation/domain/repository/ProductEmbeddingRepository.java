package com.biddy.recommendation.domain.repository;

import com.biddy.recommendation.domain.model.ProductEmbedding;

import java.util.List;
import java.util.Optional;

public interface ProductEmbeddingRepository {

    ProductEmbedding save(ProductEmbedding embedding);

    Optional<ProductEmbedding> findById(Long productId);

    List<Long> findNearestProductIds(float[] queryVector, List<Long> excludeProductIds, List<String> categories, int limit);
}
