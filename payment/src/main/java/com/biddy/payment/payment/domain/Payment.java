package com.biddy.payment.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(length = 255)
    private String pgTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    private LocalDateTime completedAt;

    private LocalDateTime failedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Payment() {
    }

    private Payment(
            Long orderId,
            Long userId,
            Long sellerId,
            Long amount,
            PaymentMethod paymentMethod,
            String pgTransactionId
    ) {
        this.orderId = orderId;
        this.userId = userId;
        this.sellerId = sellerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.pgTransactionId = pgTransactionId;
        this.status = PaymentStatus.PENDING;
    }

    public static Payment create(
            Long orderId,
            Long userId,
            Long sellerId,
            Long amount,
            PaymentMethod paymentMethod,
            String pgTransactionId
    ) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다.");
        }
        if (sellerId == null) {
            throw new IllegalArgumentException("판매자 ID는 필수입니다.");
        }
        return new Payment(orderId, userId, sellerId, amount, paymentMethod, pgTransactionId);
    }

    public void startProcessing() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("대기 중인 결제만 처리할 수 있습니다.");
        }
        this.status = PaymentStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete(String pgTransactionId) {
        this.pgTransactionId = pgTransactionId;
        this.status = PaymentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public void refund() {
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return this.status == PaymentStatus.COMPLETED;
    }

    @jakarta.persistence.PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public Long getAmount() {
        return amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getPgTransactionId() {
        return pgTransactionId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
