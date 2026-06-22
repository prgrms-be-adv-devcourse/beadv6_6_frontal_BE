package com.biddy.payment.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "deposit_accounts")
public class DepositAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private Long balance;

    @Version
    private Long version;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected DepositAccount() {
    }

    private DepositAccount(Long userId) {
        this.userId = userId;
        this.balance = 0L;
    }

    public static DepositAccount open(Long userId) {
        return new DepositAccount(userId);
    }

    public void increase(long amount) {
        validatePositive(amount);
        this.balance += amount;
        this.updatedAt = LocalDateTime.now();
    }

    public void decrease(long amount) {
        validatePositive(amount);
        if (this.balance < amount) {
            throw new IllegalStateException("예치금 잔액이 부족합니다.");
        }
        this.balance -= amount;
        this.updatedAt = LocalDateTime.now();
    }

    public void adjust(long signedAmount) {
        long adjusted = this.balance + signedAmount;
        if (adjusted < 0) {
            throw new IllegalStateException("조정 후 예치금 잔액은 0보다 작을 수 없습니다.");
        }
        this.balance = adjusted;
        this.updatedAt = LocalDateTime.now();
    }

    private void validatePositive(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        }
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

    public Long getBalance() {
        return balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
