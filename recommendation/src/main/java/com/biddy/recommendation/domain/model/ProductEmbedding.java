package com.biddy.recommendation.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ProductEmbedding {

    private final Long productId;
    private float[] embedding;
    private String sourceText;
    private String category;
    private LocalDateTime updatedAt;

    private ProductEmbedding(Long productId, float[] embedding, String sourceText, String category, LocalDateTime updatedAt) {
        this.productId = productId;
        this.embedding = embedding;
        this.sourceText = sourceText;
        this.category = category;
        this.updatedAt = updatedAt;
    }

    public static ProductEmbedding create(Long productId, float[] embedding, String sourceText, String category) {
        return new ProductEmbedding(productId, embedding, sourceText, category, LocalDateTime.now());
    }

    public static ProductEmbedding reconstruct(Long productId, float[] embedding, String sourceText, String category, LocalDateTime updatedAt) {
        return new ProductEmbedding(productId, embedding, sourceText, category, updatedAt);
    }

    public boolean isStale(String currentSourceText) {
        return !this.sourceText.equals(currentSourceText);
    }

    public void refresh(float[] embedding, String sourceText, String category) {
        this.embedding = embedding;
        this.sourceText = sourceText;
        this.category = category;
        this.updatedAt = LocalDateTime.now();
    }
}
