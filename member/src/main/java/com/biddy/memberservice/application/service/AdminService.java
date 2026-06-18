package com.biddy.memberservice.application.service;

import com.biddy.memberservice.application.dto.response.AdminMemberResponse;
import com.biddy.memberservice.domain.enums.WithdrawalStatus;
import com.biddy.memberservice.domain.model.Member;
import com.biddy.memberservice.domain.model.Withdrawal;
import com.biddy.memberservice.domain.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
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

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

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
    }

    // 예치금 강제 조정
    @Transactional
    public void adjustMemberBalanceForce(Long memberId, Long amount, String reason) {
        // 존재하는 회원인지 검증
        memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        try {
            var event = java.util.Map.of(
                    "memberId", memberId,
                    "amount", amount,       // 양수면 증액, 음수면 차감
                    "reason", reason
            );
            String message = objectMapper.writeValueAsString(event);

            // "balance-modification" 토픽으로 전송
            kafkaTemplate.send("balance-modification", message);
            log.info("🎯 예치금 강제 조정 Kafka 이벤트 발행: topic=balance-modification, memberId={}, amount={}", memberId, amount);

        } catch (JsonProcessingException e) {
            log.error("예치금 조정 이벤트 직렬화 실패 - memberId: {}", memberId, e);
            throw new RuntimeException("예치금 조정 중 알 수 없는 오류가 발생했습니다.", e);
        }
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
    }
}
