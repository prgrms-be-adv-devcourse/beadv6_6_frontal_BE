package com.biddy.auction.auction.application.dto;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.auction.domain.model.AuctionStatus;
import com.biddy.auction.bid.domain.model.Bid;

import java.time.LocalDateTime;

/**
 * 경매 상세 조회 결과 DTO.
 *
 * <p>경매 상태 데이터만 포함. 상품 정보(name, brand 등)는
 * API Gateway/BFF에서 productId로 Product Service를 조회하여 조합한다.</p>
 */
public record AuctionDetailResult(
        String auctionId,
        Long productId,
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
        TopBidderInfo topBidder,
        boolean isWatching,
        Long myHighestBid
) {

    /** 최고 입찰자 정보 (bidderId + 금액만, 닉네임은 Gateway 조합) */
    public record TopBidderInfo(Long bidderId, Long amount) {
    }

    public static AuctionDetailResult from(Auction auction, Bid topBid,
                                           boolean isWatching, int watcherCount, Long myHighestBid) {
        TopBidderInfo topBidder = topBid != null
                ? new TopBidderInfo(topBid.getBidderId(), topBid.getAmount())
                : null;

        return new AuctionDetailResult(
                auction.getAuctionId(),
                auction.getProductId(),
                auction.getSellerId(),
                auction.getStartPrice(),
                auction.getMinIncrement(),
                auction.getCurrentBid(),
                auction.getBidCount(),
                auction.getStartsAt(),
                auction.getEndsAt(),
                auction.getStatus(),
                watcherCount,
                auction.getWinnerId(),
                auction.getWinningBidId(),
                topBidder,
                isWatching,
                myHighestBid
        );
    }
}
