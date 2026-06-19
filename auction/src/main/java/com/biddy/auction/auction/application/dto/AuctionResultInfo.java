package com.biddy.auction.auction.application.dto;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.bid.domain.model.Bid;

import java.time.LocalDateTime;

/**
 * 낙찰/유찰 결과 DTO.
 *
 * @param auctionId       경매 ID
 * @param type            결과 유형 (SOLD, UNSOLD)
 * @param winnerId        낙찰자 ID (유찰이면 null)
 * @param finalBid        최종 낙찰가 (유찰이면 null)
 * @param totalBids       총 입찰 수
 * @param endedAt         경매 종료 시각
 * @param paymentDeadline 결제 기한 (낙찰 시 endsAt + 24h, 유찰이면 null)
 */
public record AuctionResultInfo(
        String auctionId,
        String type,
        Long winnerId,
        Long finalBid,
        Integer totalBids,
        LocalDateTime endedAt,
        LocalDateTime paymentDeadline
) {

    /** 낙찰 결과 생성 */
    public static AuctionResultInfo sold(Auction auction, Bid topBid) {
        return new AuctionResultInfo(
                auction.getAuctionId(),
                "SOLD",
                topBid.getBidderId(),
                topBid.getAmount(),
                auction.getBidCount(),
                auction.getEndsAt(),
                auction.getEndsAt().plusHours(24)
        );
    }

    /** 유찰 결과 생성 */
    public static AuctionResultInfo unsold(Auction auction) {
        return new AuctionResultInfo(
                auction.getAuctionId(),
                "UNSOLD",
                null,
                null,
                auction.getBidCount(),
                auction.getEndsAt(),
                null
        );
    }
}
