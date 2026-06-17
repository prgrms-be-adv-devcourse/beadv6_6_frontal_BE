package com.biddy.memberservice.domain.repository;

import com.biddy.memberservice.domain.model.WithdrawalRequest;
import com.biddy.memberservice.domain.enums.WithdrawalStatus;

import java.util.List;
import java.util.Optional;

public interface WithdrawalRequestRepository {
    WithdrawalRequest save(WithdrawalRequest withdrawalRequest);
    Optional<WithdrawalRequest> findByMemberId(Long memberId);
    Optional<WithdrawalRequest> findById(Long id);
    List<WithdrawalRequest> findAllByStatus(WithdrawalStatus status);
    Optional<WithdrawalRequest> findByMemberIdAndStatus(Long memberId, WithdrawalStatus status);
}
