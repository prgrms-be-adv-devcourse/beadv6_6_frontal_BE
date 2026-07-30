package com.biddy.auction.bid.presentation.dto;

import com.biddy.auction.bid.application.dto.PlaceBidResult;

/** 기존 프론트엔드를 위한 v1 입찰 응답 계약. */
public record LegacyPlaceBidResponse(
        Long bidId,
        Long amount,
        Long currentBid,
        Integer bidCount
) {
    public static LegacyPlaceBidResponse from(PlaceBidResult result) {
        return new LegacyPlaceBidResponse(
                result.bidId(),
                result.amount(),
                result.currentBid(),
                result.bidCount()
        );
    }
}
