package com.biddy.memberservice.infrastructure.persistence.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationJpaRepository extends JpaRepository<EmailVerificationJpaEntity, Long> {
    Optional<EmailVerificationJpaEntity> findByEmailAndToken(String email, String token);
    Optional<EmailVerificationJpaEntity> findTopByEmailOrderByCreatedAtDesc(String email);
    boolean existsByEmailAndVerifiedAtIsNotNull(String email);
}
