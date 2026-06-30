package com.biddy.order.order.infra.event.dto;

import java.time.LocalDateTime;

public record AuctionEndedEventPayload(
        String eventType,
        LocalDateTime timestamp,
        String auctionId,
        Long productId,
        Long sellerId,
        Long finalBid,
        Long winnerId,
        LocalDateTime paymentDeadline
) {}
