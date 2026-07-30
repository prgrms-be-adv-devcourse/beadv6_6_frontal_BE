package com.biddy.auction.bid.application.service;

import com.biddy.auction.auction.infra.websocket.AuctionWebSocketPublisher;
import com.biddy.auction.bid.application.dto.PlaceBidV2Command;
import com.biddy.auction.bid.application.dto.PlaceBidV2Result;
import com.biddy.auction.common.config.OptimisticLockRetryConfig;
import com.biddy.auction.common.metrics.OptimisticLockMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BidV2ServiceTest {

    @Mock
    private AuctionWebSocketPublisher webSocketPublisher;

    @Mock
    private OptimisticLockMetrics metrics;

    @Mock
    private BidV2TransactionService transactionService;

    private BidV2Service service;
    private PlaceBidV2Command command;

    @BeforeEach
    void setUp() {
        service = new BidV2Service(
                webSocketPublisher,
                metrics,
                transactionService,
                new OptimisticLockRetryConfig().optimisticLockRetryTemplate()
        );
        command = new PlaceBidV2Command("A-001", 42L, UUID.randomUUID(), 5L, 510000L);
    }

    @Test
    @DisplayName("신규 v2 입찰 커밋 후 WebSocket을 발행한다")
    void placeBid_newCommit_publishesWebSocket() {
        PlaceBidV2Result committed = result(false);
        given(transactionService.executeBidTransaction(command)).willReturn(committed);

        PlaceBidV2Result result = service.placeBid(command);

        assertThat(result).isEqualTo(committed);
        verify(webSocketPublisher).publishBid("A-001", 510000L, 6, 42L);
    }

    @Test
    @DisplayName("멱등 재응답은 중복 WebSocket 이벤트를 발행하지 않는다")
    void placeBid_idempotentReplay_doesNotPublishWebSocket() {
        PlaceBidV2Result replay = result(true);
        given(transactionService.executeBidTransaction(command)).willReturn(replay);

        PlaceBidV2Result result = service.placeBid(command);

        assertThat(result.idempotentReplay()).isTrue();
        verify(webSocketPublisher, never()).publishBid(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyLong()
        );
    }

    private PlaceBidV2Result result(boolean replay) {
        return new PlaceBidV2Result(
                101L,
                command.requestId(),
                6L,
                510000L,
                510000L,
                520000L,
                6,
                replay
        );
    }
}
