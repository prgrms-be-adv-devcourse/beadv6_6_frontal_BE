package com.biddy.memberservice.application.dto.response;

import com.biddy.memberservice.domain.model.Member;
import com.biddy.memberservice.domain.enums.MemberRole;
import com.biddy.memberservice.domain.enums.MemberStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberResponse {

    private Long id;
    private String email;
    private String nickname;
    private String profileImage;
    private String phone;
    private Boolean emailVerified;
    private MemberRole role;
    private MemberStatus status;
    private LocalDateTime createdAt;

    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .phone(member.getPhone())
                .emailVerified(member.getEmailVerified())
                .role(member.getRole())
                .status(member.getStatus())
                .createdAt(member.getCreatedAt())
                .build();
    }
}
