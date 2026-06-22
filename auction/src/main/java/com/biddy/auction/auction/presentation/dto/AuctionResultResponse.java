package com.biddy.auction.auction.presentation.dto;

import com.biddy.auction.auction.application.dto.AuctionResultInfo;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 낙찰 결과 API 응답 DTO.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuctionResultResponse(
        String auctionId,
        UUID productId,
        String type,
        Long winnerId,
        Long winningBidId,
        Long finalBid,
        Integer totalBids,
        LocalDateTime endedAt,
        LocalDateTime paymentDeadline
) {

    public static AuctionResultResponse from(AuctionResultInfo info) {
        return new AuctionResultResponse(
                info.auctionId(),
                info.productId(),
                info.type(),
                info.winnerId(),
                info.winningBidId(),
                info.finalBid(),
                info.totalBids(),
                info.endedAt(),
                info.paymentDeadline()
        );
    }
}
