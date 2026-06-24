package com.biddy.memberservice.application.scheduler;

import com.biddy.memberservice.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {
    private final RefreshTokenRepository refreshTokenRepository;

    // 매일 새벽 4시에 만료된 refresh token 삭제
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteByExpiredAtBefore(LocalDateTime.now());
        log.info("만료된 refresh token 정리 완료");
    }
}
