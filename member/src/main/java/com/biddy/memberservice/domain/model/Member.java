package com.biddy.memberservice.domain.model;

import com.biddy.memberservice.domain.enums.MemberRole;
import com.biddy.memberservice.domain.enums.MemberStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Member {

    private Long id;
    private String email;
    private String password;
    private String nickname;
    private String phone;
    private Boolean emailVerified;
    private MemberRole role;
    private MemberStatus status;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Member create(String email, String encodedPassword,
                                String nickname, String phone) {
        return Member.builder()
                .email(email)
                .password(encodedPassword)
                .nickname(nickname)
                .phone(phone)
                .emailVerified(false)
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .build();
    }

    public void updateNickname(String nickname) { this.nickname = nickname; }
    public void updatePassword(String encodedPassword) { this.password = encodedPassword; }
    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
        this.deletedAt = LocalDateTime.now();
    }

    public void suspend() {
        this.status = MemberStatus.SUSPENDED;
    }
}
