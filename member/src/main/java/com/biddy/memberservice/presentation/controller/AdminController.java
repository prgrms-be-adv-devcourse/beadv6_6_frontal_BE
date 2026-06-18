package com.biddy.memberservice.presentation.controller;

import com.biddy.memberservice.application.dto.response.AdminMemberResponse;
import com.biddy.memberservice.application.service.AdminService;
import com.biddy.memberservice.domain.model.Withdrawal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // 회원 조회
    @GetMapping("/members")
    public ResponseEntity<List<AdminMemberResponse>> getAllMembers() {
        return ResponseEntity.ok(adminService.getAllMembers());
    }

    // 탈퇴 조회
    @GetMapping("/withdrawals")
    public ResponseEntity<List<Withdrawal>> getPendingWithdrawals() {
        return ResponseEntity.ok(adminService.getPendingWithdrawals());
    }

    // 탈퇴 승인
    @PostMapping("/withdrawals/{memberId}/approve")
    public ResponseEntity<Void> approveWithdrawal(@PathVariable Long memberId) {
        adminService.approveWithdrawal(memberId);
        return ResponseEntity.ok().build();
    }

    // 회원 추방
    @PostMapping("/members/{memberId}/ban")
    public ResponseEntity<Void> banMember(@PathVariable Long memberId) {
        adminService.banMember(memberId);
        return ResponseEntity.ok().build();
    }

    // 예치금 강제 조정
    @PostMapping("/{memberId}/balance")
    public ResponseEntity<String> modifyMemberBalance(
            @PathVariable Long memberId,
            @RequestParam Long amount, // 5000 입력시 증액, -5000 입력시 차감
            @RequestParam String reason
    ) {
        adminService.adjustMemberBalanceForce(memberId, amount, reason);
        return ResponseEntity.ok("예치금 조정 요청 카프카 발행 완료: memberId=" + memberId + ", amount=" + amount);
    }
}