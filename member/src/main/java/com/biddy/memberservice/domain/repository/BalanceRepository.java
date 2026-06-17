package com.biddy.memberservice.domain.repository;

import com.biddy.memberservice.domain.model.Balance;

import java.util.Optional;

public interface BalanceRepository {
    Balance save(Balance balance);
    Optional<Balance> findByMemberId(Long memberId);
    void deleteByMemberId(Long memberId);
}
