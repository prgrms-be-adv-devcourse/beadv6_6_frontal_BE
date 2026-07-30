package com.biddy.auction.outbox.scheduler;

import com.biddy.auction.outbox.application.OutboxRelayBatchProcessor;
import com.biddy.auction.outbox.application.OutboxRelayBatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxRelaySchedulerTest {

    @Mock
    private OutboxRelayBatchProcessor batchProcessor;

    private OutboxRelayScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OutboxRelayScheduler(batchProcessor);
        ReflectionTestUtils.setField(scheduler, "maxBatchesPerRun", 10);
    }

    @Test
    @DisplayName("처리할 이벤트가 없을 때까지 새 트랜잭션 배치를 반복한다")
    void relayOutboxEvents_repeatsUntilEmpty() {
        given(batchProcessor.relayBatch())
                .willReturn(new OutboxRelayBatchResult(2, 2, 0))
                .willReturn(new OutboxRelayBatchResult(1, 1, 0))
                .willReturn(OutboxRelayBatchResult.empty());

        scheduler.relayOutboxEvents();

        verify(batchProcessor, times(3)).relayBatch();
    }

    @Test
    @DisplayName("발행 실패가 있으면 같은 실행에서 즉시 재시도하지 않는다")
    void relayOutboxEvents_failureStopsCurrentRun() {
        given(batchProcessor.relayBatch()).willReturn(new OutboxRelayBatchResult(1, 0, 1));

        scheduler.relayOutboxEvents();

        verify(batchProcessor).relayBatch();
    }
}
