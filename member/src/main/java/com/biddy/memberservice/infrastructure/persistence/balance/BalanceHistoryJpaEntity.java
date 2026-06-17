package com.biddy.memberservice.infrastructure.persistence.balance;

import com.biddy.memberservice.domain.enums.BalanceType;
import com.biddy.memberservice.domain.model.BalanceHistory;
import com.biddy.memberservice.infrastructure.persistence.member.MemberJpaEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "balance_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BalanceHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberJpaEntity member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BalanceType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceSnapshot;

    private String description;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public BalanceHistory toDomain() {
        return BalanceHistory.builder()
                .id(this.id)
                .member(this.member.toDomain())
                .type(this.type)
                .amount(this.amount)
                .balanceSnapshot(this.balanceSnapshot)
                .description(this.description)
                .createdAt(this.createdAt)
                .build();
    }

    public static BalanceHistoryJpaEntity from(BalanceHistory history) {
        BalanceHistoryJpaEntity e = new BalanceHistoryJpaEntity();
        e.id = history.getId();
        e.member = MemberJpaEntity.from(history.getMember());
        e.type = history.getType();
        e.amount = history.getAmount();
        e.balanceSnapshot = history.getBalanceSnapshot();
        e.description = history.getDescription();
        e.createdAt = history.getCreatedAt();
        return e;
    }
}
