package com.biddy.memberservice.presentation.controller;

import com.biddy.memberservice.application.dto.request.UpdateNicknameRequest;
import com.biddy.memberservice.application.dto.request.UpdatePasswordRequest;
import com.biddy.memberservice.application.dto.response.MemberResponse;
import com.biddy.memberservice.application.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyInfo(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(memberService.getMyInfo(memberId));
    }

    @PatchMapping("/me/nickname")
    public ResponseEntity<Void> updateNickname(@AuthenticationPrincipal Long memberId,
                                               @Valid @RequestBody UpdateNicknameRequest request) {
        memberService.updateNickname(memberId, request.getNickname());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> updatePassword(@AuthenticationPrincipal Long memberId,
                                               @Valid @RequestBody UpdatePasswordRequest request) {
        memberService.updatePassword(memberId, request.getCurrentPassword(),
                request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Long memberId) {
        memberService.withdraw(memberId);
        return ResponseEntity.ok().build();
    }
}
