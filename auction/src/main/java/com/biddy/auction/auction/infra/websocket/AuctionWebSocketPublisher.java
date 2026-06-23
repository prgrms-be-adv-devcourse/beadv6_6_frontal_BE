package com.biddy.auction.auction.infra.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 경매 WebSocket 메시지 발행기.
 *
 * <p>{@code /topic/auctions/{auctionId}} 채널로 메시지를 브로드캐스트한다.
 * 해당 경매를 구독 중인 모든 클라이언트에게 실시간으로 전달된다.</p>
 *
 * <p>사용 시점:
 * <ul>
 *   <li>입찰 성공 후 — BID 메시지 (현재가, 입찰 수 갱신)</li>
 *   <li>경매 종료 시 — ENDED 메시지 (낙찰자, 최종가)</li>
 * </ul></p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 입찰 발생 메시지를 경매 구독자에게 브로드캐스트한다.
     *
     * @param auctionId  경매 ID
     * @param currentBid 갱신된 현재 최고가
     * @param bidCount   갱신된 총 입찰 수
     * @param bidderId   입찰자 ID
     */
    public void publishBid(String auctionId, Long currentBid, Integer bidCount, Long bidderId) {
        AuctionWebSocketMessage message = AuctionWebSocketMessage.bid(currentBid, bidCount, bidderId);
        String destination = "/topic/auctions/" + auctionId;
        messagingTemplate.convertAndSend(destination, message);
        log.debug("WebSocket BID published: auctionId={}, currentBid={}, bidCount={}", auctionId, currentBid, bidCount);
    }

    /**
     * 경매 종료 (낙찰) 메시지를 경매 구독자에게 브로드캐스트한다.
     *
     * @param auctionId 경매 ID
     * @param winnerId  낙찰자 ID
     * @param finalBid  최종 낙찰가
     */
    public void publishEnded(String auctionId, Long winnerId, Long finalBid) {
        AuctionWebSocketMessage message = AuctionWebSocketMessage.ended(winnerId, finalBid);
        String destination = "/topic/auctions/" + auctionId;
        messagingTemplate.convertAndSend(destination, message);
        log.info("WebSocket ENDED published: auctionId={}, winnerId={}, finalBid={}", auctionId, winnerId, finalBid);
    }

    /**
     * 경매 종료 (유찰) 메시지를 경매 구독자에게 브로드캐스트한다.
     *
     * @param auctionId 경매 ID
     */
    public void publishUnsold(String auctionId) {
        AuctionWebSocketMessage message = AuctionWebSocketMessage.unsold();
        String destination = "/topic/auctions/" + auctionId;
        messagingTemplate.convertAndSend(destination, message);
        log.info("WebSocket UNSOLD published: auctionId={}", auctionId);
    }
}
