package com.biddy.auction.bid.application.service;

import com.biddy.auction.auction.infra.websocket.AuctionWebSocketPublisher;
import com.biddy.auction.bid.application.dto.PlaceBidV2Command;
import com.biddy.auction.bid.application.dto.PlaceBidV2Result;
import com.biddy.auction.bid.application.usecase.BidV2UseCase;
import com.biddy.auction.common.exception.BusinessException;
import com.biddy.auction.common.exception.ErrorCode;
import com.biddy.auction.common.metrics.OptimisticLockMetrics;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** API v2 입찰의 낙관적 락 재시도와 커밋 후 알림을 조정한다. */
@Service
@Slf4j
public class BidV2Service implements BidV2UseCase {

    private static final int MAX_ATTEMPTS = 3;

    private final AuctionWebSocketPublisher webSocketPublisher;
    private final OptimisticLockMetrics metrics;
    private final BidV2TransactionService transactionService;
    private final RetryTemplate optimisticLockRetryTemplate;

    public BidV2Service(
            AuctionWebSocketPublisher webSocketPublisher,
            OptimisticLockMetrics metrics,
            BidV2TransactionService transactionService,
            @Qualifier("optimisticLockRetryTemplate") RetryTemplate optimisticLockRetryTemplate
    ) {
        this.webSocketPublisher = webSocketPublisher;
        this.metrics = metrics;
        this.transactionService = transactionService;
        this.optimisticLockRetryTemplate = optimisticLockRetryTemplate;
    }

    @Override
    public PlaceBidV2Result placeBid(PlaceBidV2Command command) {
        Timer.Sample sample = metrics.startBidProcessing();
        long startedAt = System.nanoTime();
        int[] attempts = {1};

        try {
            PlaceBidV2Result result = optimisticLockRetryTemplate.execute(context -> {
                int attempt = context.getRetryCount() + 1;
                attempts[0] = attempt;
                if (attempt > 1) {
                    metrics.recordRetryAttempt(command.auctionId(), attempt);
                }

                try {
                    return transactionService.executeBidTransaction(command);
                } catch (OptimisticLockingFailureException | OptimisticLockException exception) {
                    metrics.recordOptimisticLockConflict(command.auctionId());
                    throw exception;
                }
            });

            if (attempts[0] > 1) {
                metrics.recordRetrySuccess(
                        command.auctionId(),
                        attempts[0],
                        Duration.ofNanos(System.nanoTime() - startedAt)
                );
            }

            if (!result.idempotentReplay()) {
                publishCommittedBid(command, result);
            }
            return result;
        } catch (BusinessException exception) {
            throw exception;
        } catch (OptimisticLockingFailureException | OptimisticLockException exception) {
            metrics.recordRetryFailure(command.auctionId(), MAX_ATTEMPTS);
            throw new BusinessException(ErrorCode.BID_CONCURRENT_MODIFICATION);
        } finally {
            metrics.recordBidProcessing(sample);
        }
    }

    private void publishCommittedBid(PlaceBidV2Command command, PlaceBidV2Result result) {
        try {
            webSocketPublisher.publishBid(
                    command.auctionId(),
                    result.currentBid(),
                    result.bidCount(),
                    command.bidderId()
            );
        } catch (RuntimeException exception) {
            log.error("v2 입찰 커밋 후 WebSocket 발행 실패 - 경매: {}, 입찰ID: {}",
                    command.auctionId(), result.bidId(), exception);
        }
    }
}
