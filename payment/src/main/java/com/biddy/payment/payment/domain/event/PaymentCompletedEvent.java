package com.biddy.payment.payment.domain.event;

import com.biddy.payment.payment.domain.PaymentMethod;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID eventId,
        Long paymentId,
        Long orderId,
        Long buyerId,
        Long sellerId,
        Long amount,
        PaymentMethod paymentMethod,
        LocalDateTime paidAt
) {
}
