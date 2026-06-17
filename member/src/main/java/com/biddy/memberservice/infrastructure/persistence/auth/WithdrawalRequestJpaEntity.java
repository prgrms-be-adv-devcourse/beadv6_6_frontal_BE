package com.biddy.memberservice.infrastructure.persistence.auth;

import com.biddy.memberservice.domain.enums.WithdrawalStatus;
import com.biddy.memberservice.domain.model.WithdrawalRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawal_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawalRequestJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WithdrawalStatus status;

    @CreationTimestamp
    private LocalDateTime requestedAt;

    private LocalDateTime approvedAt;

    public WithdrawalRequest toDomain() {
        return WithdrawalRequest.builder()
                .id(this.id)
                .memberId(this.memberId)
                .status(this.status)
                .requestedAt(this.requestedAt)
                .approvedAt(this.approvedAt)
                .build();
    }

    public static WithdrawalRequestJpaEntity from(WithdrawalRequest wr) {
        WithdrawalRequestJpaEntity e = new WithdrawalRequestJpaEntity();
        e.id = wr.getId();
        e.memberId = wr.getMemberId();
        e.status = wr.getStatus();
        e.requestedAt = wr.getRequestedAt();
        e.approvedAt = wr.getApprovedAt();
        return e;
    }
}
