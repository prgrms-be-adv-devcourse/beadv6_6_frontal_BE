package com.biddy.auction.auction.application.dto;

import com.biddy.auction.auction.domain.model.Auction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 경매 피드 조회 결과 DTO.
 *
 * <p>경매 상태 데이터만 포함. 상품 정보(name, brand 등)는 포함하지 않으며,
 * API Gateway/BFF에서 productId로 Product Service를 조회하여 조합한다.</p>
 */
public record AuctionFeedResult(
        String auctionId,
        UUID productId,
        Long sellerId,
        Long startPrice,
        Long minIncrement,
        Long currentBid,
        Integer bidCount,
        LocalDateTime endsAt,
        Integer watcherCount,
        String status
) {

    /** Auction Entity -> AuctionFeedResult 변환 */
    public static AuctionFeedResult from(Auction auction) {
        return new AuctionFeedResult(
                auction.getAuctionId(),
                auction.getProductId(),
                auction.getSellerId(),
                auction.getStartPrice(),
                auction.getMinIncrement(),
                auction.getCurrentBid(),
                auction.getBidCount(),
                auction.getEndsAt(),
                auction.getWatcherCount(),
                auction.getStatus().name()
        );
    }
}
