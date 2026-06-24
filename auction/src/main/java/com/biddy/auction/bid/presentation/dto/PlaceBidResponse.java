package com.biddy.auction.bid.presentation.dto;

import com.biddy.auction.bid.application.dto.PlaceBidResult;

/**
 * 입찰 API 응답 DTO.
 *
 * @param bidId      생성된 입찰 ID
 * @param amount     입찰 금액
 * @param currentBid 갱신된 현재 최고가
 * @param bidCount   갱신된 총 입찰 수
 */
public record PlaceBidResponse(
        Long bidId,
        Long amount,
        Long currentBid,
        Integer bidCount
) {

    public static PlaceBidResponse from(PlaceBidResult result) {
        return new PlaceBidResponse(
                result.bidId(),
                result.amount(),
                result.currentBid(),
                result.bidCount()
        );
    }
}
