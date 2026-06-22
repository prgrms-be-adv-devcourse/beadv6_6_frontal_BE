package com.biddy.memberservice.infrastructure.persistence.auth;

import com.biddy.memberservice.domain.model.EmailVerification;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerificationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 255)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    private LocalDateTime verifiedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public EmailVerification toDomain() {
        return EmailVerification.builder()
                .id(this.id)
                .email(this.email)
                .token(this.token)
                .expiredAt(this.expiredAt)
                .verifiedAt(this.verifiedAt)
                .createdAt(this.createdAt)
                .build();
    }

    public static EmailVerificationJpaEntity from(EmailVerification ev) {
        EmailVerificationJpaEntity e = new EmailVerificationJpaEntity();
        e.id = ev.getId();
        e.email = ev.getEmail();
        e.token = ev.getToken();
        e.expiredAt = ev.getExpiredAt();
        e.verifiedAt = ev.getVerifiedAt();
        e.createdAt = ev.getCreatedAt();
        return e;
    }
}
