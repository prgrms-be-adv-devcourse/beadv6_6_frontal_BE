package com.biddy.memberservice.infrastructure.persistence.member;

import com.biddy.memberservice.domain.model.Member;
import com.biddy.memberservice.domain.enums.MemberRole;
import com.biddy.memberservice.domain.enums.MemberStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    private LocalDateTime deletedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Member toDomain() {
        return Member.builder()
                .id(this.id)
                .email(this.email)
                .password(this.password)
                .nickname(this.nickname)
                .phone(this.phone)
                .emailVerified(this.emailVerified)
                .role(this.role)
                .status(this.status)
                .deletedAt(this.deletedAt)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    public static MemberJpaEntity from(Member member) {
        MemberJpaEntity e = new MemberJpaEntity();
        e.id = member.getId();
        e.email = member.getEmail();
        e.password = member.getPassword();
        e.nickname = member.getNickname();
        e.phone = member.getPhone();
        e.emailVerified = member.getEmailVerified();
        e.role = member.getRole();
        e.status = member.getStatus();
        e.deletedAt = member.getDeletedAt();
        e.createdAt = member.getCreatedAt();
        e.updatedAt = member.getUpdatedAt();
        return e;
    }
}
