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

        return productEmbeddingRepository.save(embedding);
    }

    private String buildSourceText(ProductDto product) {
        return String.join(" ",
                nullToEmpty(product.name()),
                nullToEmpty(product.category()),
                nullToEmpty(product.brand()),
                nullToEmpty(product.description()));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
