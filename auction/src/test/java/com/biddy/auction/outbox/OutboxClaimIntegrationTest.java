package com.biddy.auction.outbox;

import com.biddy.auction.outbox.domain.OutboxEvent;
import com.biddy.auction.outbox.domain.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OutboxClaimIntegrationTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    @DisplayName("같은 집합체에서는 가장 오래된 PENDING 이벤트만 선점 후보가 된다")
    void claimPendingAggregateHeads_returnsOneHeadPerAggregate() {
        OutboxEvent auctionAFirst = save("A-001", "payload-a1");
        save("A-001", "payload-a2");
        OutboxEvent auctionBFirst = save("A-002", "payload-b1");

        List<Long> claimedIds = new TransactionTemplate(transactionManager).execute(status ->
                outboxEventRepository.claimPendingAggregateHeads(100).stream()
                        .map(OutboxEvent::getId)
                        .toList());

        assertThat(claimedIds).containsExactly(auctionAFirst.getId(), auctionBFirst.getId());
    }

    @Test
    @DisplayName("다른 Pod가 첫 이벤트를 잠그면 같은 집합체의 다음 이벤트는 건너뛰고 다른 집합체를 선점한다")
    void claimPendingAggregateHeads_skipLockedWithoutReorderingAggregate() throws Exception {
        save("A-001", "payload-a1");
        OutboxEvent auctionASecond = save("A-001", "payload-a2");
        OutboxEvent auctionBFirst = save("A-002", "payload-b1");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstClaimed = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        try {
            Future<Long> firstPod = executor.submit(() -> transactionTemplate.execute(status -> {
                Long claimedId = outboxEventRepository.claimPendingAggregateHeads(1).getFirst().getId();
                firstClaimed.countDown();
                await(releaseFirst);
                return claimedId;
            }));

            assertThat(firstClaimed.await(5, TimeUnit.SECONDS)).isTrue();
            Future<Long> secondPod = executor.submit(() -> transactionTemplate.execute(status ->
                    outboxEventRepository.claimPendingAggregateHeads(1).getFirst().getId()));

            Long secondClaimedId = secondPod.get(5, TimeUnit.SECONDS);
            releaseFirst.countDown();
            firstPod.get(5, TimeUnit.SECONDS);

            assertThat(secondClaimedId).isEqualTo(auctionBFirst.getId());
            assertThat(secondClaimedId).isNotEqualTo(auctionASecond.getId());
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private OutboxEvent save(String aggregateId, String payload) {
        return outboxEventRepository.saveAndFlush(OutboxEvent.builder()
                .aggregateType("BID")
                .aggregateId(aggregateId)
                .topic("auction.bid.accepted")
                .payload(payload)
                .build());
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("테스트 잠금 대기가 중단되었습니다.", exception);
        }
    }
}
