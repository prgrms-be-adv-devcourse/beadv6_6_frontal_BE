package com.biddy.recommendation.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MatchNotification {

    private final Long memberId;
    private final Long productId;
    private final String message;
    private final LocalDateTime createdAt;

    private MatchNotification(Long memberId, Long productId, String message, LocalDateTime createdAt) {
        this.memberId = memberId;
        this.productId = productId;
        this.message = message;
        this.createdAt = createdAt;
    }

    public static MatchNotification create(Long memberId, Long productId, String message) {
        return new MatchNotification(memberId, productId, message, LocalDateTime.now());
    }

    public static MatchNotification reconstruct(Long memberId, Long productId, String message, LocalDateTime createdAt) {
        return new MatchNotification(memberId, productId, message, createdAt);
    }
}
