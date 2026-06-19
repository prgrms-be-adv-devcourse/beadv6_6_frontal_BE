package com.biddy.auction.auction.presentation.dto;

import com.biddy.auction.auction.application.dto.AuctionDetailResult;
import com.biddy.auction.auction.domain.model.AuctionStatus;

import java.time.LocalDateTime;

/**
 * 경매 상세 조회 API 응답 DTO.
 *
 * <p>application 레이어의 {@code AuctionDetailResult}를 HTTP 응답에 맞게 변환한다.
 * 내부 비즈니스 모델이 API에 직접 노출되지 않도록 보호한다.</p>
 */
public record AuctionDetailResponse(
        String auctionId,
        String name,
        String edition,
        String brand,
        String category,
        String description,
        String thumbnailUrl,
        Long startPrice,
        Long minIncrement,
        Long currentBid,
        Integer bidCount,
        LocalDateTime endsAt,
        AuctionStatus status,
        Integer watcherCount,
        TopBidderResponse topBidder,
        boolean isWatching,
        Long myHighestBid
) {

    /** 최고 입찰자 응답 */
    public record TopBidderResponse(Long collectorId, String nickname) {

        static TopBidderResponse from(AuctionDetailResult.TopBidderInfo info) {
            if (info == null) return null;
            return new TopBidderResponse(info.collectorId(), info.nickname());
        }
    }

    public static AuctionDetailResponse from(AuctionDetailResult result) {
        return new AuctionDetailResponse(
                result.auctionId(),
                result.name(),
                result.edition(),
                result.brand(),
                result.category(),
                result.description(),
                result.thumbnailUrl(),
                result.startPrice(),
                result.minIncrement(),
                result.currentBid(),
                result.bidCount(),
                result.endsAt(),
                result.status(),
                result.watcherCount(),
                TopBidderResponse.from(result.topBidder()),
                result.isWatching(),
                result.myHighestBid()
        );
    }
}
