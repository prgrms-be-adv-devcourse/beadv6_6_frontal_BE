package com.biddy.auction.auction.application.scheduler;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.auction.domain.repository.AuctionRepository;
import com.biddy.auction.auction.infra.websocket.AuctionWebSocketPublisher;
import com.biddy.auction.bid.domain.model.Bid;
import com.biddy.auction.bid.domain.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 경매 종료 스케줄러.
 *
 * <p>1초 간격으로 종료 시각이 지난 LIVE 경매를 조회하여
 * 낙찰 또는 유찰 처리를 수행한다.</p>
 *
 * <p>처리 흐름:
 * <ol>
 *   <li>ends_at <= NOW() AND status = LIVE 경매 조회</li>
 *   <li>입찰 내역 존재 여부 확인</li>
 *   <li>입찰 있음 → 낙찰 (ENDED + WebSocket ENDED push)</li>
 *   <li>입찰 없음 → 유찰 (ENDED + WebSocket UNSOLD push)</li>
 * </ol></p>
 *
 * <p>{@code @Transactional}을 {@code processExpiredAuctions()}에 직접 선언하여
 * Spring 프록시가 트랜잭션을 관리하도록 한다.
 * 같은 클래스 내부 호출은 프록시를 타지 않으므로, 진입점 메서드에 선언해야 한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionCloseScheduler {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AuctionWebSocketPublisher webSocketPublisher;

    /**
     * 1초 간격으로 만료 경매를 조회하여 종료 처리한다.
     *
     * <p>트랜잭션 내에서 auction.close()를 호출하면
     * JPA dirty checking에 의해 status=ENDED가 자동으로 DB에 반영된다.
     * 다음 스케줄러 실행 시 이미 ENDED인 경매는 조회되지 않는다.</p>
     */
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processExpiredAuctions() {
        List<Auction> expired = auctionRepository.findExpiredLiveAuctions(LocalDateTime.now());

        if (expired.isEmpty()) {
            return;
        }

        log.info("종료 대상 경매 {}건 발견", expired.size());

        for (Auction auction : expired) {
            try {
                closeAuction(auction);
            } catch (Exception e) {
                log.error("경매 종료 처리 실패: auctionId={}", auction.getAuctionId(), e);
            }
        }
    }

    /**
     * 개별 경매를 종료 처리한다.
     * auction.close()로 status를 ENDED로 변경하고,
     * 입찰 유무에 따라 낙찰/유찰을 분기한다.
     */
    private void closeAuction(Auction auction) {
        auction.close();

        if (auction.hasBids()) {
            handleAwarded(auction);
        } else {
            handleUnsold(auction);
        }
    }

    /**
     * 낙찰 처리.
     * 최고 입찰자를 확인하고 WebSocket ENDED 메시지를 브로드캐스트한다.
     */
    private void handleAwarded(Auction auction) {
        Bid topBid = bidRepository.findTopByAuctionId(auction.getAuctionId())
                .orElse(null);

        Long winnerId = topBid != null ? topBid.getBidderId() : null;
        Long finalBid = topBid != null ? topBid.getAmount() : auction.getCurrentBid();

        webSocketPublisher.publishEnded(auction.getAuctionId(), winnerId, finalBid);

        log.info("낙찰 처리 완료: auctionId={}, winnerId={}, finalBid={}",
                auction.getAuctionId(), winnerId, finalBid);
    }

    /**
     * 유찰 처리.
     * WebSocket UNSOLD 메시지를 브로드캐스트한다.
     */
    private void handleUnsold(Auction auction) {
        webSocketPublisher.publishUnsold(auction.getAuctionId());

        log.info("유찰 처리 완료: auctionId={}", auction.getAuctionId());
    }
}
