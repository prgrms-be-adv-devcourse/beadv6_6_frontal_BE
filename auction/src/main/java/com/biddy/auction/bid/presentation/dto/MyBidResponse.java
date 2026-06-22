package com.biddy.auction.bid.presentation.dto;

import com.biddy.auction.bid.application.dto.MyBidResult;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 내 입찰 참여 목록 API 응답 DTO.
 * 상품 정보 미포함 (Gateway 조합).
 */
public record MyBidResponse(
        String auctionId,
        UUID productId,
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
                result.productId(),
                result.status(),
                result.currentBid(),
                result.endsAt(),
                result.myHighestBid(),
                result.isTopBidder(),
                result.bidCount()
        );
    }
}
