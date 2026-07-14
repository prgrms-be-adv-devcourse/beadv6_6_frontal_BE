package com.biddy.recommendation.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserInterest {

    private Long id;
    private final Long memberId;
    private final String keyword;
    private final float[] embedding;
    private final LocalDateTime createdAt;

    private UserInterest(Long id, Long memberId, String keyword, float[] embedding, LocalDateTime createdAt) {
        this.id = id;
        this.memberId = memberId;
        this.keyword = keyword;
        this.embedding = embedding;
        this.createdAt = createdAt;
    }

    public static UserInterest create(Long memberId, String keyword, float[] embedding) {
        return new UserInterest(null, memberId, keyword, embedding, LocalDateTime.now());
    }

    public static UserInterest reconstruct(Long id, Long memberId, String keyword, float[] embedding, LocalDateTime createdAt) {
        return new UserInterest(id, memberId, keyword, embedding, createdAt);
    }

    public void assignId(Long id) {
        this.id = id;
    }
}
