package com.biddy.auction.bid.application.service;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.auction.infra.websocket.AuctionWebSocketPublisher;
import com.biddy.auction.bid.application.dto.PlaceBidCommand;
import com.biddy.auction.bid.application.dto.PlaceBidResult;
import com.biddy.auction.common.config.OptimisticLockRetryConfig;
import com.biddy.auction.common.exception.BusinessException;
import com.biddy.auction.common.exception.ErrorCode;
import com.biddy.auction.common.metrics.OptimisticLockMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BidServiceOptimisticLockTest {

    @Mock
    private AuctionWebSocketPublisher webSocketPublisher;

    @Mock
    private OptimisticLockMetrics metrics;

    @Mock
    private BidTransactionService transactionService;

    private BidService bidService;
    private PlaceBidCommand command;
    private PlaceBidResult committedResult;

    @BeforeEach
    void setUp() {
        bidService = new BidService(
                webSocketPublisher,
                metrics,
                transactionService,
                new OptimisticLockRetryConfig().optimisticLockRetryTemplate()
        );
        command = new PlaceBidCommand("A-001", 42L, UUID.randomUUID(), 5L, 550000L);
        committedResult = new PlaceBidResult(
                101L, command.requestId(), 6L, 510000L, 510000L, 520000L, 6, false
        );
    }

    @Test
    void committedBidPublishesWebSocketOnce() {
        given(transactionService.executeBidTransaction(command)).willReturn(committedResult);

        assertThat(bidService.placeBid(command)).isEqualTo(committedResult);

        verify(transactionService).executeBidTransaction(command);
        verify(webSocketPublisher).publishBid("A-001", 510000L, 6, 42L);
    }

    @Test
    void businessExceptionDoesNotRetry() {
        BusinessException failure = new BusinessException(ErrorCode.BID_PRICE_CHANGED);
        given(transactionService.executeBidTransaction(command)).willThrow(failure);

        assertThatThrownBy(() -> bidService.placeBid(command)).isSameAs(failure);

        verify(transactionService).executeBidTransaction(command);
        verify(metrics, never()).recordRetryAttempt("A-001", 2);
    }

    @Test
    void optimisticConflictRetriesAndSucceeds() {
        ObjectOptimisticLockingFailureException conflict =
                new ObjectOptimisticLockingFailureException(Auction.class, "A-001");
        given(transactionService.executeBidTransaction(command))
                .willThrow(conflict)
                .willReturn(committedResult);

        assertThat(bidService.placeBid(command)).isEqualTo(committedResult);

        verify(transactionService, times(2)).executeBidTransaction(command);
        verify(metrics).recordOptimisticLockConflict("A-001");
        verify(metrics).recordRetryAttempt("A-001", 2);
    }

    @Test
    void exhaustedOptimisticConflictsReturnBusinessConflict() {
        ObjectOptimisticLockingFailureException conflict =
                new ObjectOptimisticLockingFailureException(Auction.class, "A-001");
        given(transactionService.executeBidTransaction(command)).willThrow(conflict);

        assertThatThrownBy(() -> bidService.placeBid(command))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.BID_CONCURRENT_MODIFICATION);

        verify(transactionService, times(3)).executeBidTransaction(command);
        verify(metrics, times(3)).recordOptimisticLockConflict("A-001");
        verify(metrics).recordRetryFailure("A-001", 3);
    }

    @Test
    void webSocketFailureDoesNotChangeCommittedResult() {
        given(transactionService.executeBidTransaction(command)).willReturn(committedResult);
        willThrow(new RuntimeException("broker unavailable"))
                .given(webSocketPublisher)
                .publishBid("A-001", 510000L, 6, 42L);

        assertThat(bidService.placeBid(command)).isEqualTo(committedResult);
    }
}
