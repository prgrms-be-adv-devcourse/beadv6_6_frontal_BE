package com.biddy.memberservice.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailVerification {

    private Long id;
    private String email;
    private String token;
    private LocalDateTime expiredAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;

    public static EmailVerification create(String email, String token, LocalDateTime expiredAt) {
        return EmailVerification.builder()
                .email(email)
                .token(token)
                .expiredAt(expiredAt)
                .build();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiredAt);
    }

    public void verify() {
        this.verifiedAt = LocalDateTime.now();
    }
}
