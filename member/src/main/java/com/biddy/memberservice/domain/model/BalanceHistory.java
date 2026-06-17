package com.biddy.memberservice.domain.model;

import com.biddy.memberservice.domain.enums.BalanceType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BalanceHistory {

    private Long id;
    private Member member;
    private BalanceType type;
    private BigDecimal amount;
    private BigDecimal balanceSnapshot;
    private String description;
    private LocalDateTime createdAt;

    public static BalanceHistory of(Member member, BalanceType type,
                                    BigDecimal amount, BigDecimal snapshot,
                                    String description) {
        return BalanceHistory.builder()
                .member(member)
                .type(type)
                .amount(amount)
                .balanceSnapshot(snapshot)
                .description(description)
                .build();
    }
}