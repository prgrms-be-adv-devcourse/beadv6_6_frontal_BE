package com.biddy.memberservice.domain.model;

import com.biddy.memberservice.domain.enums.WithdrawalStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Withdrawal {

    private Long id;
    private Long memberId;
    private WithdrawalStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;

    public static Withdrawal create(Long memberId) {
        return Withdrawal.builder()
                .memberId(memberId)
                .status(WithdrawalStatus.PENDING)
                .build();
    }

    public void approve() {
        this.status = WithdrawalStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }
}
