package com.biddy.memberservice.application.service;

import com.biddy.memberservice.application.dto.response.AdminMemberResponse;
import com.biddy.memberservice.domain.enums.WithdrawalStatus;
import com.biddy.memberservice.domain.model.Balance;
import com.biddy.memberservice.domain.model.Member;
import com.biddy.memberservice.domain.model.WithdrawalRequest;
import com.biddy.memberservice.domain.repository.BalanceRepository;
import com.biddy.memberservice.domain.repository.MemberRepository;
import com.biddy.memberservice.domain.repository.RefreshTokenRepository;
import com.biddy.memberservice.domain.repository.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService{

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BalanceRepository balanceRepository;

    // 회원 탈퇴 조회
    public List<WithdrawalRequest> getPendingWithdrawals() {
        return withdrawalRequestRepository.findAllByStatus(WithdrawalStatus.PENDING);
    }

    // 회원 탈퇴 승인
    @Transactional
    public void approveWithdrawal(Long memberId) {
        WithdrawalRequest request = withdrawalRequestRepository
                .findByMemberIdAndStatus(memberId, WithdrawalStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("탈퇴 요청을 찾을 수 없습니다."));

        request.approve();
        withdrawalRequestRepository.save(request);
        refreshTokenRepository.deleteByMemberId(memberId);
        balanceRepository.deleteByMemberId(memberId);
        memberRepository.deleteById(memberId);
    }

    // 회원 조회
    public List<AdminMemberResponse> getAllMembers() {
        return memberRepository.findAll().stream()
                .map(member -> {
                    Balance balance = balanceRepository.findByMemberId(member.getId())
                            .orElse(null);
                    return AdminMemberResponse.of(member, balance);
                })
                .toList();
    }

    // 예치금 수정
    @Transactional
    public void updateBalance(Long memberId, BigDecimal amount) {
        Balance balance = balanceRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("잔액 정보를 찾을 수 없습니다."));
        balance.setAmount(amount);
        balanceRepository.save(balance);
    }

    // 회원 추방 (강제 탈퇴)
    @Transactional
    public void banMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        member.suspend();
        memberRepository.save(member);
        refreshTokenRepository.deleteByMemberId(memberId);
    }
}
