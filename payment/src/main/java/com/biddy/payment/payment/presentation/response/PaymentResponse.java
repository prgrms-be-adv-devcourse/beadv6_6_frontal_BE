package com.biddy.payment.payment.presentation.response;

import com.biddy.payment.payment.domain.Payment;
import com.biddy.payment.payment.domain.PaymentMethod;
import com.biddy.payment.payment.domain.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentResponse(
        Long id,
        Long orderId,
        Long userId,
        Long amount,
        PaymentMethod paymentMethod,
        String pgTransactionId,
        PaymentStatus status,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PaymentCancelResponse> cancels
) {

    public static PaymentResponse from(Payment payment, List<PaymentCancelResponse> cancels) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getPgTransactionId(),
                payment.getStatus(),
                payment.getCompletedAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                cancels
        );
    }
}
