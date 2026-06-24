package com.biddy.payment.payment.presentation.response;

import com.biddy.payment.payment.domain.CancelType;
import com.biddy.payment.payment.domain.PaymentCancel;
import java.time.LocalDateTime;

public record PaymentCancelResponse(
        Long id,
        Long paymentId,
        CancelType cancelType,
        String cancelReason,
        Long cancelAmount,
        LocalDateTime processedAt
) {

    public static PaymentCancelResponse from(PaymentCancel cancel) {
        return new PaymentCancelResponse(
                cancel.getId(),
                cancel.getPaymentId(),
                cancel.getCancelType(),
                cancel.getCancelReason(),
                cancel.getCancelAmount(),
                cancel.getProcessedAt()
        );
    }
}
