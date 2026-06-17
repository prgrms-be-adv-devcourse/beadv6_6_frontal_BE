package com.biddy.memberservice.infrastructure.persistence.balance;

import com.biddy.memberservice.domain.model.BalanceHistory;
import com.biddy.memberservice.domain.repository.BalanceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class BalanceHistoryRepositoryImpl implements BalanceHistoryRepository {

    private final BalanceHistoryJpaRepository jpaRepository;

    @Override
    public BalanceHistory save(BalanceHistory history) {
        return jpaRepository.save(BalanceHistoryJpaEntity.from(history)).toDomain();
    }

    @Override
    public List<BalanceHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId) {
        return jpaRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(BalanceHistoryJpaEntity::toDomain)
                .toList();
    }
}
