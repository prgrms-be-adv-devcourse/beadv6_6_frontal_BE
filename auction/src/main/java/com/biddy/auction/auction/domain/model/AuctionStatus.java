package com.biddy.auction.auction.domain.model;

/**
 * 경매 상태.
 *
 * <p>LIVE → ENDED 단방향 전이만 허용된다.
 * 종료 처리는 스케줄러가 ends_at 기준으로 수행한다.</p>
 */
public enum AuctionStatus {
    /** 진행 중 - 입찰 가능 */
    LIVE,
    /** 종료 - 낙찰 또는 유찰 처리 완료 */
    ENDED
}
