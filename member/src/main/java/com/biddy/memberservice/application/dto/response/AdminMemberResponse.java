package com.biddy.memberservice.application.dto.response;

import com.biddy.memberservice.domain.enums.MemberRole;
import com.biddy.memberservice.domain.enums.MemberStatus;
import com.biddy.memberservice.domain.model.Balance;
import com.biddy.memberservice.domain.model.Member;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminMemberResponse {

    private Long id;
    private String email;
    private String nickname;
    private String phone;
    private Boolean emailVerified;
    private MemberRole role;
    private MemberStatus status;
    private BigDecimal balance;
    private LocalDateTime createdAt;

    public static AdminMemberResponse of(Member member, Balance balance) {
        return AdminMemberResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .phone(member.getPhone())
                .emailVerified(member.getEmailVerified())
                .role(member.getRole())
                .status(member.getStatus())
                .balance(balance != null ? balance.getAmount() : BigDecimal.ZERO)
                .createdAt(member.getCreatedAt())
                .build();
    }
}
