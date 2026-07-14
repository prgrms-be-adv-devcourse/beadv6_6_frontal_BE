package com.biddy.recommendation.application.service;

import com.biddy.recommendation.application.dto.ProductDto;
import com.biddy.recommendation.domain.model.ProductEmbedding;
import com.biddy.recommendation.domain.repository.ProductEmbeddingRepository;
import com.biddy.recommendation.infra.acl.OpenAiEmbeddingClient;
import com.biddy.recommendation.infra.acl.ProductClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductEmbeddingService {

    private final OpenAiEmbeddingClient openAiEmbeddingClient;
    private final ProductEmbeddingRepository productEmbeddingRepository;
    private final ProductClient productClient;

    public ProductEmbedding ensureEmbedding(Long productId) {
        ProductDto product = productClient.getProduct(productId);
        if (product == null) {
            throw new IllegalArgumentException("상품을 찾을 수 없습니다. productId=" + productId);
        }
        String sourceText = buildSourceText(product);

        Optional<ProductEmbedding> existing = productEmbeddingRepository.findById(productId);
        if (existing.isPresent() && !existing.get().isStale(sourceText)) {
            return existing.get();
        }

        float[] vector = openAiEmbeddingClient.embed(sourceText);
        ProductEmbedding embedding = existing
                .map(e -> {
                    e.refresh(vector, sourceText, product.category());
                    return e;
                })
                .orElseGet(() -> ProductEmbedding.create(productId, vector, sourceText, product.category()));

        // [2026-07-15] product_embedding은 product-service 소유 테이블이라 recommendation이 직접 쓰지 않고
        // product-service API로 저장을 위임함 (조회는 계속 productEmbeddingRepository로 직접 함).
        // 예전 코드: return productEmbeddingRepository.save(embedding);
        productClient.saveEmbedding(productId, vector, sourceText, product.category());
        return embedding;
    }

    private String buildSourceText(ProductDto product) {
        StringBuilder text = new StringBuilder()
                .append("이름: ").append(nullToEmpty(product.name())).append(" / ")
                .append("카테고리: ").append(nullToEmpty(product.category())).append(" / ")
                .append("브랜드: ").append(nullToEmpty(product.brand())).append(" / ")
                .append("설명: ").append(nullToEmpty(product.description()));

        if (product.checklistAnswers() != null) {
            product.checklistAnswers().forEach((question, answer) ->
                    text.append(" / ").append(question).append(": ").append(nullToEmpty(answer)));
        }

        return text.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
