package com.biddy.productservice.domain.event;

import java.util.UUID;

public record ProductRegisteredForAuctionEvent(
        UUID productId
) {}
