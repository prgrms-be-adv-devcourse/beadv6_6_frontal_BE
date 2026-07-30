package com.biddy.auction.outbox.scheduler;

import com.biddy.auction.outbox.application.OutboxRelayBatchProcessor;
import com.biddy.auction.outbox.application.OutboxRelayBatchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Outbox 이벤트를 Kafka로 발행하는 스케줄러.
 *
 * <p>주기적으로 PENDING 상태의 이벤트를 조회하여 Kafka로 발행한다.
 * 발행 성공 시 PROCESSED로, 실패 시 재시도 횟수를 증가시키고 최대 재시도 횟수 초과 시 FAILED로 변경한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "outbox.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayScheduler {

    private final OutboxRelayBatchProcessor batchProcessor;

    @Value("${outbox.scheduler.max-batches-per-run:10}")
    private int maxBatchesPerRun;

    /**
     * 주기적으로 PENDING 상태의 Outbox 이벤트를 Kafka로 발행한다.
     * 기본 200ms마다 실행되며, application.yml에서 설정 가능하다.
     */
    @Scheduled(fixedDelayString = "${outbox.scheduler.delay:200}")
    public void relayOutboxEvents() {
        int totalProcessed = 0;

        for (int batch = 0; batch < maxBatchesPerRun; batch++) {
            OutboxRelayBatchResult result = batchProcessor.relayBatch();
            totalProcessed += result.processedCount();

            if (result.claimedCount() == 0 || result.failedCount() > 0) {
                break;
            }
        }

        if (totalProcessed > 0) {
            log.info("Outbox relay 완료 - processedCount: {}", totalProcessed);
        }
    }
}
