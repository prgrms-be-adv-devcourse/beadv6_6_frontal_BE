package com.biddy.payment.payment.infrastructure.client.order;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum OrderPaymentStatus {
    PAYMENT_PENDING,
    PAYMENT_PROCESSING,
    PAID,
    PAYMENT_FAILED,
    CANCELLED,
    REFUNDED;

    @JsonCreator
    public static OrderPaymentStatus from(String value) {
        return switch (value) {
            case "PENDING", "PAYMENT_PENDING" -> PAYMENT_PENDING;
            case "PROCESSING", "PAYMENT_PROCESSING" -> PAYMENT_PROCESSING;
            case "COMPLETED", "PAID" -> PAID;
            case "CANCELLED" -> CANCELLED;
            case "PAYMENT_FAILED" -> PAYMENT_FAILED;
            case "REFUNDED" -> REFUNDED;
            default -> throw new IllegalArgumentException("지원하지 않는 주문 상태입니다: " + value);
        };
    }
}
