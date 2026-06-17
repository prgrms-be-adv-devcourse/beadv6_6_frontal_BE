package com.biddy.memberservice.infrastructure.persistence.auth;

import com.biddy.memberservice.domain.enums.WithdrawalStatus;
import com.biddy.memberservice.domain.model.WithdrawalRequest;
import com.biddy.memberservice.domain.repository.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WithdrawalRequestRepositoryImpl implements WithdrawalRequestRepository {

    private final WithdrawalRequestJpaRepository jpaRepository;

    @Override
    public WithdrawalRequest save(WithdrawalRequest withdrawalRequest) {
        return jpaRepository.save(WithdrawalRequestJpaEntity.from(withdrawalRequest)).toDomain();
    }

    @Override
    public Optional<WithdrawalRequest> findByMemberId(Long memberId) {
        return jpaRepository.findByMemberId(memberId)
                .map(WithdrawalRequestJpaEntity::toDomain);
    }

    @Override
    public Optional<WithdrawalRequest> findById(Long id) {
        return jpaRepository.findById(id)
                .map(WithdrawalRequestJpaEntity::toDomain);
    }

    @Override
    public List<WithdrawalRequest> findAllByStatus(WithdrawalStatus status) {
        return jpaRepository.findAllByStatus(status)
                .stream()
                .map(WithdrawalRequestJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<WithdrawalRequest> findByMemberIdAndStatus(Long memberId, WithdrawalStatus status) {
        return jpaRepository.findByMemberIdAndStatus(memberId, status)
                .map(WithdrawalRequestJpaEntity::toDomain);
    }
}
