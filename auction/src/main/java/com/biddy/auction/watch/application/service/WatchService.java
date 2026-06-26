package com.biddy.auction.watch.application.service;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.auction.domain.repository.AuctionRepository;
import com.biddy.auction.common.exception.BusinessException;
import com.biddy.auction.common.exception.ErrorCode;
import com.biddy.auction.watch.application.dto.MyWatchResult;
import com.biddy.auction.watch.application.dto.ToggleWatchResult;
import com.biddy.auction.watch.application.usecase.WatchUseCase;
import com.biddy.auction.watch.domain.model.AuctionWatch;
import com.biddy.auction.watch.domain.repository.WatchRepository;
import com.biddy.auction.watch.infra.redis.WatchRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 관심 경매 UseCase 구현체.
 *
 * <p>Write-Through 방식:
 * <ul>
 *   <li>POST(토글): Redis + DB 동시 기록</li>
 *   <li>GET(조회): Redis에서만 조회 (O(1))</li>
 * </ul></p>
 *
 * <p>Redis가 비었을 때(서버 재시작 등)는
 * {@code WatchCacheWarmup}이 DB에서 복원한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatchService implements WatchUseCase {

    private final WatchRepository watchRepository;
    private final AuctionRepository auctionRepository;
    private final WatchRedisRepository watchRedis;

    /**
     * 관심 경매를 토글한다 (Write-Through: Redis + DB 동시 기록).
     */
    @Override
    @Transactional
    public ToggleWatchResult toggleWatch(String auctionId, Long memberId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        Optional<AuctionWatch> existing = watchRepository.findByAuctionIdAndMemberId(auctionId, memberId);

        if (existing.isPresent()) {
            // ─── 해제: DB + Redis 동시 ───
            watchRepository.delete(existing.get());
            auction.decrementWatcherCount();          // DB 동기화
            watchRedis.removeWatch(memberId, auctionId);
            long count = watchRedis.decrementCount(auctionId);

            log.info("관심 해제: memberId={}, auctionId={}, watcherCount={}", memberId, auctionId, count);
            return new ToggleWatchResult(false, (int) count);
        } else {
            // ─── 등록: DB + Redis 동시 ───
            AuctionWatch watch = AuctionWatch.builder()
                    .auctionId(auctionId)
                    .memberId(memberId)
                    .build();
            watchRepository.save(watch);
            auction.incrementWatcherCount();           // DB 동기화
            watchRedis.addWatch(memberId, auctionId);
            long count = watchRedis.incrementCount(auctionId);

            log.info("관심 등록: memberId={}, auctionId={}, watcherCount={}", memberId, auctionId, count);
            return new ToggleWatchResult(true, (int) count);
        }
    }

    /**
     * 관심 여부 확인 — Redis SISMEMBER O(1).
     */
    @Override
    public boolean isWatching(String auctionId, Long memberId) {
        return watchRedis.isWatching(memberId, auctionId);
    }

    /**
     * 내 관심 경매 목록 — DB 페이징 조회 (정렬/페이징이 필요하므로 DB 사용).
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MyWatchResult> getMyWatches(Long memberId, int page, int size) {
        Page<AuctionWatch> watches = watchRepository.findByMemberId(memberId, PageRequest.of(page, size));

        return watches.map(watch -> {
            String aid = watch.getAuctionId();
            int count = watchRedis.getCount(aid);
            return auctionRepository.findById(aid)
                    .map(auction -> MyWatchResult.from(auction, count))
                    .orElse(null);
        });
    }
}
