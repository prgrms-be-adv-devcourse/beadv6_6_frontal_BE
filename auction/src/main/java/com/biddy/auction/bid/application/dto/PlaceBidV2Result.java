package com.biddy.auction.bid.application.dto;

import java.util.UUID;

/** 서버가 승인한 입찰과 멱등 재응답 여부. */
public record PlaceBidV2Result(
        Long bidId,
        UUID requestId,
        Long sequence,
        Long amount,
        Long currentBid,
        Long nextMinimumBid,
        Integer bidCount,
        boolean idempotentReplay
) {
}
