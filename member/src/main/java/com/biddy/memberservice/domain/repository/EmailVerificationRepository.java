package com.biddy.memberservice.domain.repository;

import com.biddy.memberservice.domain.model.EmailVerification;

import java.util.Optional;

public interface EmailVerificationRepository {
    EmailVerification save(EmailVerification emailVerification);
    Optional<EmailVerification> findByEmailAndToken(String email, String token);
    Optional<EmailVerification> findTopByEmailOrderByCreatedAtDesc(String email);
    boolean existsByEmailAndVerifiedAtIsNotNull(String email);
}
