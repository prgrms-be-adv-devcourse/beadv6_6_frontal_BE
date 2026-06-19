package com.biddy.auction.watch.presentation.dto;

import com.biddy.auction.watch.application.dto.MyWatchResult;

import java.time.LocalDateTime;

/**
 * 내 관심 경매 목록 API 응답 DTO.
 */
public record MyWatchResponse(
        String auctionId,
        String name,
        Long currentBid,
        Integer bidCount,
        LocalDateTime endsAt,
        Integer watcherCount,
        String thumbnailUrl,
        String status
) {

    public static MyWatchResponse from(MyWatchResult result) {
        return new MyWatchResponse(
                result.auctionId(),
                result.name(),
                result.currentBid(),
                result.bidCount(),
                result.endsAt(),
                result.watcherCount(),
                result.thumbnailUrl(),
                result.status()
        );
    }
}
