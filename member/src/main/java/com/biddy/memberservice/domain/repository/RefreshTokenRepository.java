package com.biddy.memberservice.domain.repository;

import com.biddy.memberservice.domain.model.RefreshToken;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken refreshToken);
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByMemberId(Long memberId);
    void deleteByMemberId(Long memberId);
    void delete(RefreshToken refreshToken);
    void deleteByExpiredAtBefore(LocalDateTime now);
}
