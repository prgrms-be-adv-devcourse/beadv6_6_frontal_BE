package com.biddy.memberservice.domain.repository;

import com.biddy.memberservice.domain.model.Withdrawal;
import com.biddy.memberservice.domain.enums.WithdrawalStatus;

import java.util.List;
import java.util.Optional;

public interface WithdrawalRequestRepository {
    Withdrawal save(Withdrawal withdrawal);
    Optional<Withdrawal> findByMemberId(Long memberId);
    Optional<Withdrawal> findById(Long id);
    List<Withdrawal> findAllByStatus(WithdrawalStatus status);
    Optional<Withdrawal> findByMemberIdAndStatus(Long memberId, WithdrawalStatus status);
}
