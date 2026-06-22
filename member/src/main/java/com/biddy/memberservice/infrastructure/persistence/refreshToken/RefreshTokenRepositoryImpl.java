package com.biddy.memberservice.infrastructure.persistence.refreshToken;

import com.biddy.memberservice.domain.model.RefreshToken;
import com.biddy.memberservice.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return jpaRepository.save(RefreshTokenJpaEntity.from(refreshToken)).toDomain();
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(token)
                .map(RefreshTokenJpaEntity::toDomain);
    }

    @Override
    public Optional<RefreshToken> findByMemberId(Long memberId) {
        return jpaRepository.findByMemberId(memberId)
                .map(RefreshTokenJpaEntity::toDomain);
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        jpaRepository.deleteByMemberId(memberId);
    }

    @Override
    public void delete(RefreshToken refreshToken) {
        jpaRepository.deleteById(refreshToken.getId());
    }

    @Override
    public void deleteByExpiredAtBefore(LocalDateTime now) {
        jpaRepository.deleteByExpiredAtBefore(now);
    }
}
