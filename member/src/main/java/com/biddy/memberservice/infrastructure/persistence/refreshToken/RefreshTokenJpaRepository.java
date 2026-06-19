package com.biddy.memberservice.infrastructure.persistence.refreshToken;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {
    Optional<RefreshTokenJpaEntity> findByToken(String token);
    Optional<RefreshTokenJpaEntity> findByMemberId(Long memberId);
    void deleteByMemberId(Long memberId);
    void deleteByToken(String token);
    void deleteByExpiredAtBefore(LocalDateTime expiredAtBefore);
}
