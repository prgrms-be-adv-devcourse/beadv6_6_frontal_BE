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
@Table(name = "payment_cancels")
public class PaymentCancel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CancelType cancelType;

    @Column(length = 255)
    private String cancelReason;

    @Column(nullable = false)
    private Long cancelAmount;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected PaymentCancel() {
    }

    private PaymentCancel(Long paymentId, CancelType cancelType, String cancelReason, Long cancelAmount) {
        this.paymentId = paymentId;
        this.cancelType = cancelType;
        this.cancelReason = cancelReason;
        this.cancelAmount = cancelAmount;
        this.processedAt = LocalDateTime.now();
    }

    public static PaymentCancel create(Long paymentId, CancelType cancelType, String cancelReason, Long cancelAmount) {
        if (cancelAmount == null || cancelAmount <= 0) {
            throw new IllegalArgumentException("취소/환불 금액은 0보다 커야 합니다.");
        }
        return new PaymentCancel(paymentId, cancelType, cancelReason, cancelAmount);
    }

    @jakarta.persistence.PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public CancelType getCancelType() {
        return cancelType;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public Long getCancelAmount() {
        return cancelAmount;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
