package com.biddy.auction.auction.infra.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * WebSocket으로 클라이언트에 push되는 경매 실시간 메시지.
 *
 * <p>입찰 발생 시 BID 타입, 경매 종료 시 ENDED 타입으로 전달된다.</p>
 *
 * <p>BID 메시지 예시:
 * <pre>{@code
 * { "type": "BID", "currentBid": 740000, "bidCount": 7,
 *   "bidder": { "bidderId": 42 } }
 * }</pre></p>
 *
 * <p>ENDED 메시지 예시:
 * <pre>{@code
 * { "type": "ENDED", "winnerId": 42, "finalBid": 740000 }
 * }</pre></p>
 *
 * @param type       메시지 타입 (BID, ENDED)
 * @param currentBid 현재 최고 입찰가 (BID 시)
 * @param bidCount   총 입찰 수 (BID 시)
 * @param bidderId   입찰자 ID (BID 시)
 * @param winnerId   낙찰자 ID (ENDED 시, 유찰이면 null)
 * @param finalBid   최종 낙찰가 (ENDED 시, 유찰이면 null)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuctionWebSocketMessage(
        String type,
        Long currentBid,
        Integer bidCount,
        Long bidderId,
        Long winnerId,
        Long finalBid
) {

    /** 입찰 발생 메시지 생성 */
    public static AuctionWebSocketMessage bid(Long currentBid, Integer bidCount, Long bidderId) {
        return new AuctionWebSocketMessage("BID", currentBid, bidCount, bidderId, null, null);
    }

    /** 경매 종료 (낙찰) 메시지 생성 */
    public static AuctionWebSocketMessage ended(Long winnerId, Long finalBid) {
        return new AuctionWebSocketMessage("ENDED", null, null, null, winnerId, finalBid);
    }

    /** 경매 종료 (유찰) 메시지 생성 */
    public static AuctionWebSocketMessage unsold() {
        return new AuctionWebSocketMessage("ENDED", null, null, null, null, null);
    }
}
