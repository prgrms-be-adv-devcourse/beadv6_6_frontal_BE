package com.biddy.auction.bid.application.dto;

/**
 * 입찰 내역 조회 조건 DTO.
 *
 * @param auctionId 조회 대상 경매 ID
 * @param page      페이지 번호 (0부터)
 * @param size      페이지 크기
 */
public record BidHistoryQuery(
        String auctionId,
        int page,
        int size
) {
}
