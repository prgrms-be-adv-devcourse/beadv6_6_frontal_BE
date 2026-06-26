package com.biddy.auction.watch.application.dto;

import com.biddy.auction.auction.domain.model.Auction;

import java.time.LocalDateTime;

/**
 * 내 관심 경매 조회 결과 DTO.
 * 상품 정보 미포함 (Gateway 조합).
 */
public record MyWatchResult(
        String auctionId,
        Long productId,
        Long currentBid,
        Integer bidCount,
        LocalDateTime endsAt,
        Integer watcherCount,
        String status
) {

    public static MyWatchResult from(Auction auction, int watcherCount) {
        return new MyWatchResult(
                auction.getAuctionId(),
                auction.getProductId(),
                auction.getCurrentBid(),
                auction.getBidCount(),
                auction.getEndsAt(),
                watcherCount,
                auction.getStatus().name()
        );
    }
}
