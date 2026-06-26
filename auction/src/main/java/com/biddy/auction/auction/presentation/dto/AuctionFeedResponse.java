package com.biddy.auction.auction.presentation.dto;

import com.biddy.auction.auction.application.dto.AuctionFeedResult;

import java.time.LocalDateTime;

/**
 * 경매 피드 API 응답 DTO.
 *
 * <p>경매 상태 데이터만 포함. 상품 정보(name, brand 등)는
 * API Gateway/BFF에서 productId로 Product Service를 조회하여 조합한다.</p>
 */
public record AuctionFeedResponse(
        String auctionId,
        Long productId,
        Long sellerId,
        Long startPrice,
        Long minIncrement,
        Long currentBid,
        Integer bidCount,
        LocalDateTime endsAt,
        Integer watcherCount,
        String status
) {

    public static AuctionFeedResponse from(AuctionFeedResult result) {
        return new AuctionFeedResponse(
                result.auctionId(),
                result.productId(),
                result.sellerId(),
                result.startPrice(),
                result.minIncrement(),
                result.currentBid(),
                result.bidCount(),
                result.endsAt(),
                result.watcherCount(),
                result.status()
        );
    }
}
