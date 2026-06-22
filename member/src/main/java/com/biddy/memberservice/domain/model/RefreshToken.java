package com.biddy.memberservice.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RefreshToken {

    private Long id;
    private Member member;
    private String token;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;

    public static RefreshToken create(Member member, String token, LocalDateTime expiredAt) {
        return RefreshToken.builder()
                .member(member)
                .token(token)
                .expiredAt(expiredAt)
                .build();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiredAt);
    }
}
