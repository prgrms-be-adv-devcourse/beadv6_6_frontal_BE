package com.biddy.auction.bid.presentation.dto;

import com.biddy.auction.bid.application.dto.BidHistoryResult;

import java.time.LocalDateTime;

/**
 * 입찰 내역 API 응답 DTO.
 *
 * <p>application 레이어의 {@code BidHistoryResult}를 API 응답 형태로 변환한다.
 * 내부 비즈니스 모델이 HTTP 응답에 직접 노출되지 않도록 보호한다.</p>
 */
public record BidHistoryResponse(
        BidderResponse bidder,
        Long amount,
        LocalDateTime bidAt
) {

    public record BidderResponse(Long collectorId, String nickname) {

        static BidderResponse from(BidHistoryResult.BidderInfo bidderInfo) {
            return new BidderResponse(bidderInfo.collectorId(), bidderInfo.nickname());
        }
    }

    public static BidHistoryResponse from(BidHistoryResult result) {
        return new BidHistoryResponse(
                BidderResponse.from(result.bidder()),
                result.amount(),
                result.bidAt()
        );
    }
}
