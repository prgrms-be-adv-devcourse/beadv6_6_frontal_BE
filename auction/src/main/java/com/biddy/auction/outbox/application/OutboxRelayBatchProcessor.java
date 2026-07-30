package com.biddy.auction.outbox.application;

import com.biddy.auction.outbox.domain.OutboxEvent;
import com.biddy.auction.outbox.domain.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** Outbox 한 배치를 선점하고 Kafka acknowledgment 및 상태 변경을 한 트랜잭션에서 처리한다. */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayBatchProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${outbox.scheduler.batch-size:100}")
    private int batchSize;

    @Value("${outbox.scheduler.max-retry-count:5}")
    private int maxRetryCount;

    @Value("${outbox.scheduler.send-timeout-seconds:10}")
    private long sendTimeoutSeconds;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OutboxRelayBatchResult relayBatch() {
        List<OutboxEvent> claimedEvents = outboxEventRepository.claimPendingAggregateHeads(batchSize);
        if (claimedEvents.isEmpty()) {
            return OutboxRelayBatchResult.empty();
        }

        int processedCount = 0;
        int failedCount = 0;

        for (OutboxEvent event : claimedEvents) {
            try {
                // aggregateId를 key로 고정해 동일 경매 이벤트가 같은 Kafka partition에 기록되도록 한다.
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                        .get(sendTimeoutSeconds, TimeUnit.SECONDS);
                event.markAsProcessed();
                processedCount++;
                log.debug("Outbox 발행 완료 - id: {}, eventId: {}, topic: {}",
                        event.getId(), event.getEventId(), event.getTopic());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                event.markAsFailed(exception, maxRetryCount);
                failedCount++;
                log.error("Outbox 발행 실패 - id: {}, eventId: {}, retryCount: {}",
                        event.getId(), event.getEventId(), event.getRetryCount(), exception);
                break;
            } catch (Exception exception) {
                event.markAsFailed(exception, maxRetryCount);
                failedCount++;
                log.error("Outbox 발행 실패 - id: {}, eventId: {}, retryCount: {}",
                        event.getId(), event.getEventId(), event.getRetryCount(), exception);
            }
        }

        outboxEventRepository.saveAll(claimedEvents);
        return new OutboxRelayBatchResult(claimedEvents.size(), processedCount, failedCount);
    }
}
