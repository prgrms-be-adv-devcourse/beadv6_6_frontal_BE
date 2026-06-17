package com.biddy.memberservice.application.service;

import com.biddy.memberservice.application.dto.response.BalanceResponse;
import com.biddy.memberservice.domain.model.Balance;
import com.biddy.memberservice.domain.model.BalanceHistory;
import com.biddy.memberservice.domain.repository.BalanceHistoryRepository;
import com.biddy.memberservice.domain.repository.BalanceRepository;
import com.biddy.memberservice.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BalanceService{

    private final BalanceRepository balanceRepository;
    private final BalanceHistoryRepository balanceHistoryRepository;
    private final MemberRepository memberRepository;

    public BalanceResponse getBalanceDashboard(Long memberId) {
        // 현재 잔액 조회
        Balance balance = balanceRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("잔액 정보를 찾을 수 없습니다."));

        // 회원의 거래 내역 최신순으로 조회
        List<BalanceHistory> histories = balanceHistoryRepository.findByMemberIdOrderByCreatedAtDesc(memberId);

        return BalanceResponse.of(balance, histories);
    }

}
