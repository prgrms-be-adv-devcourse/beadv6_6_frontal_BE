package com.biddy.memberservice.infrastructure.persistence.balance;

import com.biddy.memberservice.domain.model.Balance;
import com.biddy.memberservice.domain.repository.BalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BalanceRepositoryImpl implements BalanceRepository {

    private final BalanceJpaRepository jpaRepository;

    @Override
    public Balance save(Balance balance) {
        return jpaRepository.save(BalanceJpaEntity.from(balance)).toDomain();
    }

    @Override
    public Optional<Balance> findByMemberId(Long memberId) {
        return jpaRepository.findByMemberId(memberId).map(BalanceJpaEntity::toDomain);
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        jpaRepository.deleteByMemberId(memberId);
    }
}
