package com.biddy.auction.bid.application.dto;

import java.time.LocalDateTime;

/**
 * 내 입찰 참여 경매 조회 결과 DTO.
 *
 * @param auctionId    경매 ID
 * @param name         상품명
 * @param thumbnailUrl 썸네일 URL
 * @param status       경매 상태
 * @param currentBid   현재 최고가
 * @param endsAt       종료 시각
 * @param myHighestBid 내 최고 입찰 금액
 * @param isTopBidder  내가 현재 최고 입찰자인지
 * @param bidCount     총 입찰 수
 */
public record MyBidResult(
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
}
