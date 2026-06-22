package com.biddy.payment.settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_items")
public class SettlementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long settlementId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long saleAmount;

    @Column(nullable = false)
    private Long commissionAmount;

    @Column(nullable = false)
    private Long netAmount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected SettlementItem() {
    }

    private SettlementItem(Long settlementId, Long orderId, Long saleAmount, Long commissionAmount, Long netAmount) {
        this.settlementId = settlementId;
        this.orderId = orderId;
        this.saleAmount = saleAmount;
        this.commissionAmount = commissionAmount;
        this.netAmount = netAmount;
    }

    public static SettlementItem create(Long settlementId, Long orderId, Long saleAmount, Long commissionAmount, Long netAmount) {
        return new SettlementItem(settlementId, orderId, saleAmount, commissionAmount, netAmount);
    }

    @jakarta.persistence.PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getSettlementId() {
        return settlementId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getSaleAmount() {
        return saleAmount;
    }

    public Long getCommissionAmount() {
        return commissionAmount;
    }

    public Long getNetAmount() {
        return netAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
