package com.biddy.auction.watch.infra.redis;

import com.biddy.auction.watch.domain.model.AuctionWatch;
import com.biddy.auction.watch.infra.persistence.WatchJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 서버 시작 시 DB → Redis 관심 경매 데이터 Warm-up.
 *
 * <p>Redis가 비어있을 때(서버 재시작, Redis 재시작 등)
 * DB의 auction_watch 테이블에서 데이터를 읽어 Redis에 복원한다.</p>
 *
 * <p>복원 대상:
 * <ul>
 *   <li>{@code watch:user:{memberId}} — 회원별 관심 SET</li>
 *   <li>{@code watch:auction:{auctionId}:count} — 경매별 관심 수</li>
 * </ul></p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WatchCacheWarmup {

    private final WatchJpaRepository watchJpaRepository;
    private final WatchRedisRepository watchRedis;

    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        log.info("[Watch Warm-up] DB → Redis 관심 경매 캐시 복원 시작...");

        List<AuctionWatch> allWatches = watchJpaRepository.findAll();

        if (allWatches.isEmpty()) {
            log.info("[Watch Warm-up] DB에 관심 데이터 없음. 스킵.");
            return;
        }

        // 경매별 카운트 집계
        Map<String, Integer> countMap = new HashMap<>();

        for (AuctionWatch watch : allWatches) {
            // 회원별 SET에 추가
            watchRedis.addWatch(watch.getMemberId(), watch.getAuctionId());

            // 경매별 카운트 집계
            countMap.merge(watch.getAuctionId(), 1, Integer::sum);
        }

        // 경매별 카운트 Redis에 설정
        countMap.forEach(watchRedis::setCount);

        log.info("[Watch Warm-up] 완료: 관심 {}건, 경매 {}개 복원",
                allWatches.size(), countMap.size());
    }
}
