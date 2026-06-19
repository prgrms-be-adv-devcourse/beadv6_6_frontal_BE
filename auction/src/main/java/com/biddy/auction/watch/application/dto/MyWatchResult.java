package com.biddy.auction.watch.application.dto;

import com.biddy.auction.auction.domain.model.Auction;

import java.time.LocalDateTime;

/**
 * 내 관심 경매 조회 결과 DTO.
 *
 * @param auctionId    경매 ID
 * @param name         상품명
 * @param currentBid   현재 최고가
 * @param bidCount     총 입찰 수
 * @param endsAt       종료 시각
 * @param watcherCount 관심 등록 수
 * @param thumbnailUrl 썸네일 URL
 * @param status       경매 상태
 */
public record MyWatchResult(
        String auctionId,
        String name,
        Long currentBid,
        Integer bidCount,
        LocalDateTime endsAt,
        Integer watcherCount,
        String thumbnailUrl,
        String status
) {

    public static MyWatchResult from(Auction auction) {
        return new MyWatchResult(
                auction.getAuctionId(),
                auction.getName(),
                auction.getCurrentBid(),
                auction.getBidCount(),
                auction.getEndsAt(),
                auction.getWatcherCount(),
                auction.getThumbnailUrl(),
                auction.getStatus().name()
        );
    }
}
