package com.biddy.auction.bid.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 내 입찰 참여 경매 조회 결과 DTO.
 * 상품 정보 미포함 (Gateway 조합).
 */
public record MyBidResult(
        String auctionId,
        UUID productId,
        String status,
        Long currentBid,
        LocalDateTime endsAt,
        Long myHighestBid,
        boolean isTopBidder,
        Integer bidCount
) {
}
