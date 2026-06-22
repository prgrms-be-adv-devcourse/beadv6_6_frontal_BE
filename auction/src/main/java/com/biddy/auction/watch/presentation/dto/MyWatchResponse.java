package com.biddy.auction.watch.presentation.dto;

import com.biddy.auction.watch.application.dto.MyWatchResult;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 내 관심 경매 목록 API 응답 DTO.
 * 상품 정보 미포함 (Gateway 조합).
 */
public record MyWatchResponse(
        String auctionId,
        UUID productId,
        Long currentBid,
        Integer bidCount,
        LocalDateTime endsAt,
        Integer watcherCount,
        String status
) {

    public static MyWatchResponse from(MyWatchResult result) {
        return new MyWatchResponse(
                result.auctionId(),
                result.productId(),
                result.currentBid(),
                result.bidCount(),
                result.endsAt(),
                result.watcherCount(),
                result.status()
        );
    }
}
