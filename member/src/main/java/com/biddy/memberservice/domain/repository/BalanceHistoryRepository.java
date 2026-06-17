package com.biddy.memberservice.domain.repository;

import com.biddy.memberservice.domain.model.BalanceHistory;

import java.util.List;

public interface BalanceHistoryRepository {
    BalanceHistory save(BalanceHistory history);
    List<BalanceHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
