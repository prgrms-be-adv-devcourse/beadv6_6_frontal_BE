package com.biddy.auction.outbox.application;

/** 한 Outbox 선점·발행 트랜잭션의 처리 결과. */
public record OutboxRelayBatchResult(
        int claimedCount,
        int processedCount,
        int failedCount
) {
    public static OutboxRelayBatchResult empty() {
        return new OutboxRelayBatchResult(0, 0, 0);
    }
}
