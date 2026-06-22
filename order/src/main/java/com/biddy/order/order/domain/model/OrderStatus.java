package com.biddy.order.order.domain.model;

public enum OrderStatus {
    PENDING,     // 주문 대기
    PROCESSING,  // 결제 처리 중
    COMPLETED,   // 주문 완료
    CANCELLED    // 주문 취소
}
