package com.biddy.payment.wallet.domain;

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
@Table(name = "deposit_transactions")
public class DepositTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DepositTransactionType type;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long balanceAfter;

    @Column(nullable = false, length = 80)
    private String referenceType;

    @Column(length = 255)
    private String referenceId;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected DepositTransaction() {
    }

    private DepositTransaction(
            Long userId,
            DepositTransactionType type,
            Long amount,
            Long balanceAfter,
            String referenceType,
            String referenceId,
            String reason
    ) {
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.reason = reason;
    }

    public static DepositTransaction record(
            Long userId,
            DepositTransactionType type,
            Long amount,
            Long balanceAfter,
            String referenceType,
            String referenceId,
            String reason
    ) {
        return new DepositTransaction(userId, type, amount, balanceAfter, referenceType, referenceId, reason);
    }

    @jakarta.persistence.PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public DepositTransactionType getType() {
        return type;
    }

    public Long getAmount() {
        return amount;
    }

    public Long getBalanceAfter() {
        return balanceAfter;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
