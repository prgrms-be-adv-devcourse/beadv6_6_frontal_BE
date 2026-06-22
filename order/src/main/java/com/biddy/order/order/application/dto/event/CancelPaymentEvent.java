package com.biddy.order.order.application.dto.event;

public record CancelPaymentEvent(
        Long orderId,
        Long userId
) {
}
