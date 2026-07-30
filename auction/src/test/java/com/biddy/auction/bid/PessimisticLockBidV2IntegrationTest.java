package com.biddy.auction.bid;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.auction.domain.model.AuctionStatus;
import com.biddy.auction.auction.domain.repository.AuctionRepository;
import com.biddy.auction.bid.application.dto.PlaceBidV2Command;
import com.biddy.auction.bid.application.usecase.BidV2UseCase;
import com.biddy.auction.bid.domain.repository.BidRepository;
import com.biddy.auction.common.exception.BusinessException;
import com.biddy.auction.common.exception.ErrorCode;
import com.biddy.auction.outbox.domain.OutboxEvent;
import com.biddy.auction.outbox.domain.OutboxEventRepository;
import com.biddy.auction.outbox.domain.OutboxStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "bid.execution-mode=pessimistic",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class PessimisticLockBidV2IntegrationTest {

    @Autowired
    private BidV2UseCase bidV2UseCase;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    @DisplayName("같은 sequence의 동시 v2 입찰은 행 잠금 순서상 한 건만 커밋된다")
    void concurrentBidsWithSameObservedSequence_commitExactlyOne() throws Exception {
        String auctionId = "PESS-" + UUID.randomUUID().toString().substring(0, 12);
        auctionRepository.save(Auction.builder()
                .auctionId(auctionId)
                .productId(System.nanoTime())
                .sellerId(1000L)
                .startPrice(10000L)
                .currentBid(10000L)
                .minIncrement(1000L)
                .status(AuctionStatus.LIVE)
                .endsAt(LocalDateTime.now().plusHours(1))
                .build());
        auctionRepository.flush();

        int requestCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger staleCount = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < requestCount; index++) {
                long bidderId = 2000L + index;
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    try {
                        bidV2UseCase.placeBid(new PlaceBidV2Command(
                                auctionId,
                                bidderId,
                                UUID.randomUUID(),
                                0L,
                                11000L
                        ));
                        successCount.incrementAndGet();
                    } catch (BusinessException exception) {
                        if (exception.getErrorCode() == ErrorCode.BID_STALE_STATE) {
                            staleCount.incrementAndGet();
                            return null;
                        }
                        throw exception;
                    }
                    return null;
                }));
            }

            startLatch.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        Auction committedAuction = auctionRepository.findById(auctionId).orElseThrow();
        int storedBidCount = bidRepository.findByAuctionId(
                auctionId,
                PageRequest.of(0, requestCount)
        ).getNumberOfElements();
        List<OutboxEvent> outboxEvents = outboxEventRepository.findByAggregateIdOrderByIdAsc(auctionId);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(staleCount.get()).isEqualTo(requestCount - 1);
        assertThat(storedBidCount).isEqualTo(1);
        assertThat(outboxEvents).hasSize(1);
        assertThat(outboxEvents.getFirst().getEventId()).isNotNull();
        assertThat(outboxEvents.getFirst().getTopic()).isEqualTo("auction.bid.accepted");
        assertThat(outboxEvents.getFirst().getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(committedAuction.getCurrentBid()).isEqualTo(11000L);
        assertThat(committedAuction.getBidCount()).isEqualTo(1);
        assertThat(committedAuction.currentBidSequence()).isEqualTo(1L);
    }
}
