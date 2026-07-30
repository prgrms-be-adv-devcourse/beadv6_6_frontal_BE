package com.biddy.auction.outbox.application;

import com.biddy.auction.outbox.domain.OutboxEvent;
import com.biddy.auction.outbox.domain.OutboxEventRepository;
import com.biddy.auction.outbox.domain.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxRelayBatchProcessorTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxRelayBatchProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new OutboxRelayBatchProcessor(outboxEventRepository, kafkaTemplate);
        ReflectionTestUtils.setField(processor, "batchSize", 100);
        ReflectionTestUtils.setField(processor, "maxRetryCount", 2);
        ReflectionTestUtils.setField(processor, "sendTimeoutSeconds", 1L);
    }

    @Test
    @DisplayName("aggregateId를 Kafka key로 발행하고 acknowledgment 후 PROCESSED로 변경한다")
    void relayBatch_acknowledged_marksProcessed() {
        OutboxEvent event = event("A-001", "payload-1");
        given(outboxEventRepository.claimPendingAggregateHeads(100)).willReturn(List.of(event));
        given(kafkaTemplate.send("auction.bid.accepted", "A-001", "payload-1"))
                .willReturn(CompletableFuture.completedFuture(null));

        OutboxRelayBatchResult result = processor.relayBatch();

        assertThat(result).isEqualTo(new OutboxRelayBatchResult(1, 1, 0));
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        assertThat(event.getProcessedAt()).isNotNull();
        verify(outboxEventRepository).saveAll(List.of(event));
    }

    @Test
    @DisplayName("Kafka 발행 실패는 PENDING 상태로 다음 폴링에서 재시도한다")
    void relayBatch_sendFailure_keepsPendingBeforeRetryLimit() {
        OutboxEvent event = event("A-001", "payload-1");
        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IllegalStateException("broker unavailable"));
        given(outboxEventRepository.claimPendingAggregateHeads(100)).willReturn(List.of(event));
        given(kafkaTemplate.send("auction.bid.accepted", "A-001", "payload-1"))
                .willReturn(failedFuture);

        OutboxRelayBatchResult result = processor.relayBatch();

        assertThat(result).isEqualTo(new OutboxRelayBatchResult(1, 0, 1));
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getLastError()).contains("broker unavailable");
    }

    @Test
    @DisplayName("최대 재시도 횟수에 도달하면 FAILED로 전환한다")
    void relayBatch_retryLimit_marksFailed() {
        OutboxEvent event = event("A-001", "payload-1");
        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IllegalStateException("broker unavailable"));
        given(outboxEventRepository.claimPendingAggregateHeads(100)).willReturn(List.of(event));
        given(kafkaTemplate.send("auction.bid.accepted", "A-001", "payload-1"))
                .willReturn(failedFuture);

        processor.relayBatch();
        processor.relayBatch();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(2);
    }

    private OutboxEvent event(String aggregateId, String payload) {
        return OutboxEvent.builder()
                .aggregateType("BID")
                .aggregateId(aggregateId)
                .topic("auction.bid.accepted")
                .payload(payload)
                .build();
    }
}
