package com.biddy.recommendation.domain.repository;

import com.biddy.recommendation.domain.model.ProductEmbedding;

import java.util.List;
import java.util.Optional;

public interface ProductEmbeddingRepository {

    // [2026-07-15] product_embedding은 product-service 소유 테이블로 정리하면서 쓰기 경로 제거.
    // 저장은 이제 ProductClient.saveEmbedding()으로 product-service API를 호출해서 위임함.
    // 예전 코드: ProductEmbedding save(ProductEmbedding embedding);

    Optional<ProductEmbedding> findById(Long productId);

    List<Long> findNearestProductIds(float[] queryVector, List<Long> excludeProductIds, List<String> categories, int limit);

    List<Long> findProductIdsByCategory(String category, int limit);
}
