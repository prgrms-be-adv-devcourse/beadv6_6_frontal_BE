package com.biddy.auction.bid.infra.kafka;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.bid.domain.model.Bid;

import java.time.LocalDateTime;
import java.util.UUID;

/** Kafka {@code auction.bid.accepted} 이벤트 계약. */
public record BidAcceptedEventPayload(
        UUID eventId,
        String eventType,
        LocalDateTime timestamp,
        String auctionId,
        Long bidId,
        UUID requestId,
        Long sequence,
        Long currentBid,
        Long nextMinimumBid,
        Integer bidCount,
        Long bidderId
) {
    public static BidAcceptedEventPayload from(UUID eventId, Auction auction, Bid bid) {
        long nextMinimumBid;
        try {
            nextMinimumBid = Math.addExact(auction.getCurrentBid(), auction.getMinIncrement());
        } catch (ArithmeticException exception) {
            throw new IllegalStateException("다음 입찰가 계산 중 오버플로가 발생했습니다.", exception);
        }

        return new BidAcceptedEventPayload(
                eventId,
                "BID_ACCEPTED",
                LocalDateTime.now(),
                auction.getAuctionId(),
                bid.getBidId(),
                bid.getRequestId(),
                bid.getSequence(),
                auction.getCurrentBid(),
                nextMinimumBid,
                auction.getBidCount(),
                bid.getBidderId()
        );
    }
}
