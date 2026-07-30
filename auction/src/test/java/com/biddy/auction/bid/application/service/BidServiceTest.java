package com.biddy.auction.bid.application.service;

import com.biddy.auction.auction.infra.websocket.AuctionWebSocketPublisher;
import com.biddy.auction.bid.application.dto.PlaceBidCommand;
import com.biddy.auction.bid.application.dto.PlaceBidResult;
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
class BidServiceTest {

    @Mock
    private AuctionWebSocketPublisher webSocketPublisher;

    @Mock
    private OptimisticLockMetrics metrics;

    @Mock
    private BidTransactionService transactionService;

    private BidService service;
    private PlaceBidCommand command;

    @BeforeEach
    void setUp() {
        service = new BidService(
                webSocketPublisher,
                metrics,
                transactionService,
                new OptimisticLockRetryConfig().optimisticLockRetryTemplate()
        );
        command = new PlaceBidCommand("A-001", 42L, UUID.randomUUID(), 5L, 510000L);
    }

    @Test
    @DisplayName("신규 입찰 커밋 후 WebSocket을 발행한다")
    void placeBid_newCommit_publishesWebSocket() {
        PlaceBidResult committed = result(false);
        given(transactionService.executeBidTransaction(command)).willReturn(committed);

        PlaceBidResult result = service.placeBid(command);

        assertThat(result).isEqualTo(committed);
        verify(webSocketPublisher).publishBid("A-001", 510000L, 6, 42L);
    }

    @Test
    @DisplayName("멱등 재응답은 중복 WebSocket 이벤트를 발행하지 않는다")
    void placeBid_idempotentReplay_doesNotPublishWebSocket() {
        PlaceBidResult replay = result(true);
        given(transactionService.executeBidTransaction(command)).willReturn(replay);

        PlaceBidResult result = service.placeBid(command);

        assertThat(result.idempotentReplay()).isTrue();
        verify(webSocketPublisher, never()).publishBid(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyLong()
        );
    }

    private PlaceBidResult result(boolean replay) {
        return new PlaceBidResult(
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
