package com.biddy.productservice.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductRegisteredForAuctionEvent(
        Long productId,
        Long sellerId,
        BigDecimal startPrice,
        Integer minIncrement,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {}
