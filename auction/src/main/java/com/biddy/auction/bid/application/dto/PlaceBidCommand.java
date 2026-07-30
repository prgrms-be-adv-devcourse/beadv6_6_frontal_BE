package com.biddy.auction.bid.application.dto;

import java.util.UUID;

/** 서버 계산 입찰 API v2 명령. */
public record PlaceBidCommand(
        String auctionId,
        Long bidderId,
        UUID requestId,
        Long observedSequence,
        Long maxAcceptableAmount
) {
}
