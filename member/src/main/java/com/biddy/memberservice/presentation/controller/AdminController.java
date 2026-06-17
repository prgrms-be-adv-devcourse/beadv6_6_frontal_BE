package com.biddy.memberservice.presentation.controller;

import com.biddy.memberservice.application.dto.response.AdminMemberResponse;
import com.biddy.memberservice.application.service.AdminService;
import com.biddy.memberservice.domain.model.WithdrawalRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<List<WithdrawalRequest>> getPendingWithdrawals() {
        return ResponseEntity.ok(adminService.getPendingWithdrawals());
    }

    // 탈퇴 승인
    @PostMapping("/withdrawals/{memberId}/approve")
    public ResponseEntity<Void> approveWithdrawal(@PathVariable Long memberId) {
        adminService.approveWithdrawal(memberId);
        return ResponseEntity.ok().build();
    }

    // 예치금 수정
    @PatchMapping("/members/{memberId}/balance")
    public ResponseEntity<Void> updateBalance(@PathVariable Long memberId,
                                              @RequestBody Map<String, BigDecimal> body) {
        adminService.updateBalance(memberId, body.get("amount"));
        return ResponseEntity.ok().build();
    }

    // 회원 추방
    @PostMapping("/members/{memberId}/ban")
    public ResponseEntity<Void> banMember(@PathVariable Long memberId) {
        adminService.banMember(memberId);
        return ResponseEntity.ok().build();
    }
}