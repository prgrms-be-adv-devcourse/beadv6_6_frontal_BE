package com.biddy.auction.auction.infra.kafka;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.bid.domain.model.Bid;

import java.time.LocalDateTime;

/**
 * 경매 종료(낙찰) Kafka 이벤트 Payload.
 *
 * <p>Auction -> Order Service로 발행하는 이벤트.
 * 상품 정보는 포함하지 않으며, Order가 productId로 Product Service에서 조회한다.</p>
 *
 * <p>Topic: {@code auction.ended}</p>
 */
public record AuctionEndedEventPayload(
        String eventType,
        LocalDateTime timestamp,
        String auctionId,
        Long productId,
        Long sellerId,
        Long finalBid,
        Long winnerId,
        LocalDateTime paymentDeadline
) {

    public static AuctionEndedEventPayload from(Auction auction, Bid topBid) {
        return new AuctionEndedEventPayload(
                "AUCTION_ENDED",
                LocalDateTime.now(),
                auction.getAuctionId(),
                auction.getProductId(),
                auction.getSellerId(),
                topBid.getAmount(),
                topBid.getBidderId(),
                LocalDateTime.now().plusHours(24)
        );
    }
}
