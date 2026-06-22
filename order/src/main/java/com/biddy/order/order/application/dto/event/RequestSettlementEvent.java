package com.biddy.order.order.application.dto.event;

public record RequestSettlementEvent(
        Long orderId,
        Long userId
) {
}
