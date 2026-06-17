package com.biddy.memberservice.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Balance {

    private Long id;
    private Member member;
    private BigDecimal amount;
    private LocalDateTime updatedAt;

    public static Balance create(Member member) {
        return Balance.builder()
                .member(member)
                .amount(BigDecimal.ZERO)
                .build();
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
