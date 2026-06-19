package com.biddy.payment.payment.infrastructure.client.order;

public enum OrderPaymentStatus {
    PAYMENT_PENDING,
    PAYMENT_PROCESSING,
    PAID,
    PAYMENT_FAILED,
    CANCELLED,
    REFUNDED
}
