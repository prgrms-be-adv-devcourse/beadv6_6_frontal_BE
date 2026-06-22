package com.biddy.memberservice.infrastructure.persistence.auth;

import com.biddy.memberservice.domain.model.EmailVerification;
import com.biddy.memberservice.domain.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EmailVerificationRepositoryImpl implements EmailVerificationRepository {

    private final EmailVerificationJpaRepository jpaRepository;

    @Override
    public EmailVerification save(EmailVerification emailVerification) {
        return jpaRepository.save(EmailVerificationJpaEntity.from(emailVerification)).toDomain();
    }

    @Override
    public Optional<EmailVerification> findByEmailAndToken(String email, String token) {
        return jpaRepository.findByEmailAndToken(email, token)
                .map(EmailVerificationJpaEntity::toDomain);
    }

    @Override
    public Optional<EmailVerification> findTopByEmailOrderByCreatedAtDesc(String email) {
        return jpaRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .map(EmailVerificationJpaEntity::toDomain);
    }

    @Override
    public boolean existsByEmailAndVerifiedAtIsNotNull(String email) {
        return jpaRepository.existsByEmailAndVerifiedAtIsNotNull(email);
    }
}
