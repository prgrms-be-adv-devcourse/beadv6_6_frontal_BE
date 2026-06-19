package com.biddy.auction.auction.presentation.dto;

import com.biddy.auction.auction.application.dto.AuctionResultInfo;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * 낙찰 결과 API 응답 DTO.
 *
 * @param auctionId       경매 ID
 * @param type            결과 유형 (SOLD, UNSOLD)
 * @param winner          낙찰자 정보 (유찰이면 null)
 * @param finalBid        최종 낙찰가 (유찰이면 null)
 * @param totalBids       총 입찰 수
 * @param endedAt         경매 종료 시각
 * @param paymentDeadline 결제 기한 (낙찰 시, 유찰이면 null)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuctionResultResponse(
        String auctionId,
        String type,
        WinnerResponse winner,
        Long finalBid,
        Integer totalBids,
        LocalDateTime endedAt,
        LocalDateTime paymentDeadline
) {

    public record WinnerResponse(Long collectorId, String nickname) {
    }

    public static AuctionResultResponse from(AuctionResultInfo info) {
        WinnerResponse winner = info.winnerId() != null
                ? new WinnerResponse(info.winnerId(), null)
                : null;

        return new AuctionResultResponse(
                info.auctionId(),
                info.type(),
                winner,
                info.finalBid(),
                info.totalBids(),
                info.endedAt(),
                info.paymentDeadline()
        );
    }
}
