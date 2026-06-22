package com.biddy.auction.bid.application.dto;

import com.biddy.auction.bid.domain.model.Bid;

import java.time.LocalDateTime;

/**
 * 입찰 내역 조회 결과 DTO.
 *
 * <p>Bid Entity → BidHistoryResult 변환을 통해 엔티티 직접 노출을 방지한다.
 * nickname은 Member Service 연동(ACL) 전까지 null로 반환된다.</p>
 */
public record BidHistoryResult(
        BidderInfo bidder,
        Long amount,
        LocalDateTime bidAt
) {

    /** 입찰자 정보 (Member Service 연동 전까지 nickname은 null) */
    public record BidderInfo(Long collectorId, String nickname) {
    }

    /** Bid Entity → BidHistoryResult 변환 팩토리 메서드 */
    public static BidHistoryResult from(Bid bid) {
        return new BidHistoryResult(
                new BidderInfo(bid.getBidderId(), null),
                bid.getAmount(),
                bid.getBidAt()
        );
    }
}
