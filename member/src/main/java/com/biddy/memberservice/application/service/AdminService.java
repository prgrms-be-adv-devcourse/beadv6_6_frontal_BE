package com.biddy.memberservice.application.service;

import com.biddy.memberservice.application.dto.response.AdminMemberResponse;
import com.biddy.memberservice.application.event.MemberEventPublisher;
import com.biddy.memberservice.domain.enums.WithdrawalStatus;
import com.biddy.memberservice.domain.model.Member;
import com.biddy.memberservice.domain.model.Withdrawal;
import com.biddy.memberservice.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberEventPublisher eventPublisher;

    // 회원 탈퇴 조회
    public List<Withdrawal> getPendingWithdrawals() {
        return withdrawalRequestRepository.findAllByStatus(WithdrawalStatus.PENDING);
    }

    // 회원 탈퇴 승인
    @Transactional
    public void approveWithdrawal(Long memberId) {
        Withdrawal request = withdrawalRequestRepository
                .findByMemberIdAndStatus(memberId, WithdrawalStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("탈퇴 요청을 찾을 수 없습니다."));

        request.approve();
        withdrawalRequestRepository.save(request);
        refreshTokenRepository.deleteByMemberId(memberId);
        memberRepository.deleteById(memberId);
        eventPublisher.publishWithdraw(memberId);
        log.info("Kafka 이벤트 발행: topic=member-withdraw, memberId={}", memberId);
    }

    // 회원 조회
    public List<AdminMemberResponse> getAllMembers() {
        return memberRepository.findAll().stream()
                .map(AdminMemberResponse::of)
                .toList();
    }

    // 회원 추방 (강제 탈퇴)
    @Transactional
    public void banMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        member.suspend();
        memberRepository.save(member);
        refreshTokenRepository.deleteByMemberId(memberId);
        eventPublisher.publishWithdraw(memberId);
        log.info("Kafka 이벤트 발행: topic=member-withdraw, memberId={}", memberId);
    }
}
