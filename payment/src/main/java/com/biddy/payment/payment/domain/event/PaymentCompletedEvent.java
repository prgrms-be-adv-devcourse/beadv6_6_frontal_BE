package com.biddy.payment.payment.domain.event;

import com.biddy.payment.payment.domain.PaymentMethod;
import java.time.LocalDateTime;

public record PaymentCompletedEvent(
        Long paymentId,
        Long orderId,
        Long buyerId,
        Long sellerId,
        Long amount,
        PaymentMethod paymentMethod,
        LocalDateTime occurredAt
) {
}
