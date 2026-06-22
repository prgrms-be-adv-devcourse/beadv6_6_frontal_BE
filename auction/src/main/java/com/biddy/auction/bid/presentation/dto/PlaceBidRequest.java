package com.biddy.auction.bid.presentation.dto;

/**
 * 입찰 API 요청 DTO.
 *
 * @param amount 입찰 금액
 */
public record PlaceBidRequest(
        Long amount
) {
}
