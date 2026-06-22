package com.biddy.memberservice.infrastructure.persistence.auth;

import com.biddy.memberservice.domain.enums.WithdrawalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WithdrawalRequestJpaRepository extends JpaRepository<WithdrawalRequestJpaEntity, Long> {
    Optional<WithdrawalRequestJpaEntity> findByMemberId(Long memberId);
    List<WithdrawalRequestJpaEntity> findAllByStatus(WithdrawalStatus status);
    Optional<WithdrawalRequestJpaEntity> findByMemberIdAndStatus(Long memberId, WithdrawalStatus status);
}
