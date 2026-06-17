package com.biddy.memberservice.infrastructure.persistence.balance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BalanceJpaRepository extends JpaRepository<BalanceJpaEntity, Long> {
    Optional<BalanceJpaEntity> findByMemberId(Long memberId);
    void deleteByMemberId(Long memberId);
}
