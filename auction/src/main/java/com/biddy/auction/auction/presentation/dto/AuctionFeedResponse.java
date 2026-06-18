package com.biddy.auction.auction.presentation.dto;

import com.biddy.auction.auction.application.dto.AuctionFeedResult;

import java.time.LocalDateTime;

/**
 * 경매 피드 API 응답 DTO.
 *
 * <p>application 레이어의 {@code AuctionFeedResult}를 HTTP 응답에 맞게 변환한다.
 * API 스펙이 변경되어도 내부 비즈니스 모델에 영향을 주지 않는다.</p>
 */
public record AuctionFeedResponse(
        String auctionId,
        String name,
        String edition,
        String brand,
        Long currentBid,
        Integer bidCount,
        LocalDateTime endsAt,
        Integer watcherCount,
        String thumbnailUrl,
        SellerResponse seller
) {

    public record SellerResponse(Long collectorId, String nickname) {

        static SellerResponse from(AuctionFeedResult.SellerInfo sellerInfo) {
            return new SellerResponse(sellerInfo.collectorId(), sellerInfo.nickname());
        }
    }

    public static AuctionFeedResponse from(AuctionFeedResult result) {
        return new AuctionFeedResponse(
                result.auctionId(),
                result.name(),
                result.edition(),
                result.brand(),
                result.currentBid(),
                result.bidCount(),
                result.endsAt(),
                result.watcherCount(),
                result.thumbnailUrl(),
                SellerResponse.from(result.seller())
        );
    }
}
