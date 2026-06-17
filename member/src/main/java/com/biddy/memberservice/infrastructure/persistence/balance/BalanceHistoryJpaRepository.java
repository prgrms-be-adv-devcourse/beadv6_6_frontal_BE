package com.biddy.memberservice.infrastructure.persistence.balance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BalanceHistoryJpaRepository extends JpaRepository<BalanceHistoryJpaEntity, Long> {
    List<BalanceHistoryJpaEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
