package com.biddy.auction.auction.application.scheduler;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.auction.domain.model.AuctionStatus;
import com.biddy.auction.auction.domain.repository.AuctionRepository;
import com.biddy.auction.auction.infra.kafka.AuctionEndedEventProducer;
import com.biddy.auction.auction.infra.websocket.AuctionWebSocketPublisher;
import com.biddy.auction.bid.domain.model.Bid;
import com.biddy.auction.bid.domain.repository.BidRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionCloseSchedulerTest {

    @InjectMocks
    private AuctionCloseScheduler scheduler;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private AuctionWebSocketPublisher webSocketPublisher;

    @Mock
    private AuctionEndedEventProducer auctionEndedEventProducer;

    private Auction createExpiredAuction(String id, int bidCount) {
        return Auction.builder()
                .auctionId(id)
                .productId(UUID.randomUUID())
                .sellerId(10L)
                .startPrice(100000L)
                .minIncrement(10000L)
                .currentBid(bidCount > 0 ? 500000L : 0L)
                .bidCount(bidCount)
                .status(AuctionStatus.LIVE)
                .endsAt(LocalDateTime.now().minusMinutes(1))
                .build();
    }

    @Test
    @DisplayName("만료 경매가 없으면 아무 처리도 하지 않는다")
    void noExpiredAuctions() {
        given(auctionRepository.findExpiredLiveAuctions(any()))
                .willReturn(List.of());

        scheduler.processExpiredAuctions();

        verifyNoInteractions(webSocketPublisher);
    }

    @Test
    @DisplayName("입찰이 있는 경매는 낙찰 처리한다")
    void awardedWithBids() {
        Auction auction = createExpiredAuction("A-001", 5);
        Bid topBid = Bid.builder()
                .bidId(101L).auctionId("A-001").bidderId(42L).amount(720000L).build();

        given(auctionRepository.findExpiredLiveAuctions(any()))
                .willReturn(List.of(auction));
        given(bidRepository.findTopByAuctionId("A-001"))
                .willReturn(Optional.of(topBid));

        scheduler.processExpiredAuctions();

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ENDED);
        verify(webSocketPublisher).publishEnded("A-001", 42L, 720000L);
        verify(auctionEndedEventProducer).publish(auction, topBid);
        verify(webSocketPublisher, never()).publishUnsold(any());
    }

    @Test
    @DisplayName("입찰이 없는 경매는 유찰 처리한다")
    void unsoldWithNoBids() {
        Auction auction = createExpiredAuction("A-002", 0);

        given(auctionRepository.findExpiredLiveAuctions(any()))
                .willReturn(List.of(auction));

        scheduler.processExpiredAuctions();

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ENDED);
        verify(webSocketPublisher).publishUnsold("A-002");
        verify(webSocketPublisher, never()).publishEnded(any(), any(), any());
        verifyNoInteractions(auctionEndedEventProducer);
    }

    @Test
    @DisplayName("여러 경매를 각각 낙찰/유찰 분기 처리한다")
    void multipleExpiredAuctions() {
        Auction auction1 = createExpiredAuction("A-001", 3);
        Auction auction2 = createExpiredAuction("A-002", 0);

        given(auctionRepository.findExpiredLiveAuctions(any()))
                .willReturn(List.of(auction1, auction2));
        given(bidRepository.findTopByAuctionId("A-001"))
                .willReturn(Optional.of(
                        Bid.builder().bidId(1L).auctionId("A-001").bidderId(42L).amount(500000L).build()
                ));

        scheduler.processExpiredAuctions();

        verify(webSocketPublisher).publishEnded("A-001", 42L, 500000L);
        verify(webSocketPublisher).publishUnsold("A-002");
    }

    @Test
    @DisplayName("하나의 경매 처리 실패해도 다른 경매는 정상 처리된다")
    void exceptionDoesNotAffectOthers() {
        Auction auction1 = createExpiredAuction("A-001", 3);
        Auction auction2 = createExpiredAuction("A-002", 0);

        given(auctionRepository.findExpiredLiveAuctions(any()))
                .willReturn(List.of(auction1, auction2));
        given(bidRepository.findTopByAuctionId("A-001"))
                .willThrow(new RuntimeException("DB 오류"));

        scheduler.processExpiredAuctions();

        verify(webSocketPublisher).publishUnsold("A-002");
    }
}
