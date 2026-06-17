package com.biddy.memberservice.infrastructure.persistence.balance;

import com.biddy.memberservice.domain.model.Balance;
import com.biddy.memberservice.infrastructure.persistence.member.MemberJpaEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "balance")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BalanceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private MemberJpaEntity member;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Balance toDomain() {
        return Balance.builder()
                .id(this.id)
                .member(this.member.toDomain())
                .amount(this.amount)
                .updatedAt(this.updatedAt)
                .build();
    }

    public static BalanceJpaEntity from(Balance balance) {
        BalanceJpaEntity e = new BalanceJpaEntity();
        e.id = balance.getId();
        e.member = MemberJpaEntity.from(balance.getMember());
        e.amount = balance.getAmount();
        e.updatedAt = balance.getUpdatedAt();
        return e;
    }
}
