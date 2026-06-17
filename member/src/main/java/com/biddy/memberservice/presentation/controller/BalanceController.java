package com.biddy.memberservice.presentation.controller;

import com.biddy.memberservice.application.service.BalanceService;
import com.biddy.memberservice.domain.model.BalanceHistory;
import com.biddy.memberservice.application.dto.response.BalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members/me/balance")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;

    @GetMapping
    public ResponseEntity<BalanceResponse> getBalanceDashboard(@AuthenticationPrincipal Long memberId) {
        // 잔액 + 내역 리스트 반환
        return ResponseEntity.ok(balanceService.getBalanceDashboard(memberId));
    }
}
