package com.biddy.payment.settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlements")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private Long orderId;

    @Column(length = 7)
    private String settlementYearMonth;

    @Column(nullable = false)
    private Long totalAmount;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;

    @Column(nullable = false)
    private Long commissionAmount;

    @Column(nullable = false)
    private Long settlementAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    private LocalDateTime settledAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Settlement() {
    }

    private Settlement(
            Long userId,
            Long orderId,
            String settlementYearMonth,
            Long totalAmount,
            BigDecimal commissionRate,
            Long commissionAmount,
            Long settlementAmount
    ) {
        this.userId = userId;
        this.orderId = orderId;
        this.settlementYearMonth = settlementYearMonth;
        this.totalAmount = totalAmount;
        this.commissionRate = commissionRate;
        this.commissionAmount = commissionAmount;
        this.settlementAmount = settlementAmount;
        this.status = SettlementStatus.PENDING;
    }

    public static Settlement create(
            Long userId,
            String settlementYearMonth,
            Long totalAmount,
            BigDecimal commissionRate,
            Long commissionAmount,
            Long settlementAmount
    ) {
        return new Settlement(userId, null, settlementYearMonth, totalAmount, commissionRate, commissionAmount, settlementAmount);
    }

    public static Settlement createPending(
            Long userId,
            Long orderId,
            Long totalAmount,
            BigDecimal commissionRate,
            Long commissionAmount,
            Long settlementAmount
    ) {
        return new Settlement(userId, orderId, null, totalAmount, commissionRate, commissionAmount, settlementAmount);
    }

    public void complete() {
        if (this.status == SettlementStatus.COMPLETED) {
            return;
        }
        if (this.status == SettlementStatus.CANCELLED) {
            throw new IllegalStateException("취소된 정산은 완료할 수 없습니다.");
        }
        this.status = SettlementStatus.COMPLETED;
        this.settledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status == SettlementStatus.CANCELLED) {
            return;
        }
        if (this.status == SettlementStatus.COMPLETED) {
            throw new IllegalStateException("완료된 정산은 취소할 수 없습니다.");
        }
        this.status = SettlementStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
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

    public Long getUserId() {
        return userId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getSettlementYearMonth() {
        return settlementYearMonth;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public Long getCommissionAmount() {
        return commissionAmount;
    }

    public Long getSettlementAmount() {
        return settlementAmount;
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
