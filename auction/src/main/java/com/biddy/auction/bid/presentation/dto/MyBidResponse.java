package com.biddy.auction.bid.presentation.dto;

import com.biddy.auction.bid.application.dto.MyBidResult;

import java.time.LocalDateTime;

/**
 * 내 입찰 참여 목록 API 응답 DTO.
 */
public record MyBidResponse(
        String auctionId,
        String name,
        String thumbnailUrl,
        String status,
        Long currentBid,
        LocalDateTime endsAt,
        Long myHighestBid,
        boolean isTopBidder,
        Integer bidCount
) {

    public static MyBidResponse from(MyBidResult result) {
        return new MyBidResponse(
                result.auctionId(),
                result.name(),
                result.thumbnailUrl(),
                result.status(),
                result.currentBid(),
                result.endsAt(),
                result.myHighestBid(),
                result.isTopBidder(),
                result.bidCount()
        );
    }
}
