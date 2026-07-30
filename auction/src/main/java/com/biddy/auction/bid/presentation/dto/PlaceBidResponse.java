package com.biddy.auction.bid.presentation.dto;

import com.biddy.auction.bid.application.dto.PlaceBidResult;

import java.util.UUID;

/** 서버 계산 입찰 성공 또는 멱등 재응답. */
public record PlaceBidResponse(
        Long bidId,
        UUID requestId,
        Long sequence,
        Long amount,
        Long currentBid,
        Long nextMinimumBid,
        Integer bidCount,
        boolean idempotentReplay
) {
    public static PlaceBidResponse from(PlaceBidResult result) {
        return new PlaceBidResponse(
                result.bidId(),
                result.requestId(),
                result.sequence(),
                result.amount(),
                result.currentBid(),
                result.nextMinimumBid(),
                result.bidCount(),
                result.idempotentReplay()
        );
    }
}
