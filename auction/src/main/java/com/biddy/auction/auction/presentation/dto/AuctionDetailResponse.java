package com.biddy.auction.auction.presentation.dto;

import com.biddy.auction.auction.application.dto.AuctionDetailResult;
import com.biddy.auction.auction.domain.model.AuctionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 경매 상세 조회 API 응답 DTO.
 *
 * <p>경매 상태 데이터만 포함. 상품 정보는 Gateway 조합.</p>
 */
public record AuctionDetailResponse(
        String auctionId,
        UUID productId,
        Long sellerId,
        Long startPrice,
        Long minIncrement,
        Long currentBid,
        Integer bidCount,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        AuctionStatus status,
        Integer watcherCount,
        Long winnerId,
        Long winningBidId,
        TopBidderResponse topBidder,
        boolean isWatching,
        Long myHighestBid
) {

    /** 최고 입찰자 응답 (bidderId + 금액만) */
    public record TopBidderResponse(Long bidderId, Long amount) {

        static TopBidderResponse from(AuctionDetailResult.TopBidderInfo info) {
            if (info == null) return null;
            return new TopBidderResponse(info.bidderId(), info.amount());
        }
    }

    public static AuctionDetailResponse from(AuctionDetailResult result) {
        return new AuctionDetailResponse(
                result.auctionId(),
                result.productId(),
                result.sellerId(),
                result.startPrice(),
                result.minIncrement(),
                result.currentBid(),
                result.bidCount(),
                result.startsAt(),
                result.endsAt(),
                result.status(),
                result.watcherCount(),
                result.winnerId(),
                result.winningBidId(),
                TopBidderResponse.from(result.topBidder()),
                result.isWatching(),
                result.myHighestBid()
        );
    }
}
