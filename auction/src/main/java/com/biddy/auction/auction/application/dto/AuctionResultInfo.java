package com.biddy.auction.auction.application.dto;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.bid.domain.model.Bid;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 낙찰/유찰 결과 DTO.
 */
public record AuctionResultInfo(
        String auctionId,
        UUID productId,
        String type,
        Long winnerId,
        Long winningBidId,
        Long finalBid,
        Integer totalBids,
        LocalDateTime endedAt,
        LocalDateTime paymentDeadline
) {

    /** 낙찰 결과 생성 */
    public static AuctionResultInfo sold(Auction auction, Bid topBid) {
        return new AuctionResultInfo(
                auction.getAuctionId(),
                auction.getProductId(),
                "SOLD",
                auction.getWinnerId(),
                auction.getWinningBidId(),
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
                auction.getProductId(),
                "UNSOLD",
                null,
                null,
                null,
                auction.getBidCount(),
                auction.getEndsAt(),
                null
        );
    }
}
