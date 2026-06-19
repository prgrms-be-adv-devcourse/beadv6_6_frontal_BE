package com.biddy.auction.bid.application.service;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.auction.domain.model.AuctionStatus;
import com.biddy.auction.auction.domain.repository.AuctionRepository;
import com.biddy.auction.auction.infra.websocket.AuctionWebSocketPublisher;
import com.biddy.auction.bid.application.dto.MyBidResult;
import com.biddy.auction.bid.domain.model.Bid;
import com.biddy.auction.bid.domain.repository.BidRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MyBidServiceTest {

    @InjectMocks
    private BidService bidService;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private AuctionWebSocketPublisher webSocketPublisher;

    private Auction createAuction(String id, AuctionStatus status, Long currentBid, int bidCount) {
        return Auction.builder()
                .auctionId(id).sellerId(10L).name("상품 " + id)
                .startPrice(100000L).minIncrement(10000L)
                .currentBid(currentBid).bidCount(bidCount)
                .status(status).watcherCount(0)
                .thumbnailUrl("https://img.test.com/" + id + ".jpg")
                .endsAt(LocalDateTime.of(2026, 6, 20, 15, 0))
                .build();
    }

    @Test
    @DisplayName("내 입찰 참여 경매 목록을 조회한다")
    void getMyBids_returnsList() {
        Auction auction = createAuction("A-001", AuctionStatus.LIVE, 720000L, 5);
        Bid myBid = Bid.builder().bidId(1L).auctionId("A-001").bidderId(42L).amount(700000L).build();
        Bid topBid = Bid.builder().bidId(2L).auctionId("A-001").bidderId(99L).amount(720000L).build();

        given(bidRepository.findDistinctAuctionIdsByBidderId(42L)).willReturn(List.of("A-001"));
        given(auctionRepository.findById("A-001")).willReturn(Optional.of(auction));
        given(bidRepository.findTopByAuctionIdAndBidderId("A-001", 42L)).willReturn(Optional.of(myBid));
        given(bidRepository.findTopByAuctionId("A-001")).willReturn(Optional.of(topBid));

        Page<MyBidResult> result = bidService.getMyBids(42L, null, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        MyBidResult item = result.getContent().get(0);
        assertThat(item.auctionId()).isEqualTo("A-001");
        assertThat(item.myHighestBid()).isEqualTo(700000L);
        assertThat(item.isTopBidder()).isFalse();
        assertThat(item.currentBid()).isEqualTo(720000L);
    }

    @Test
    @DisplayName("내가 최고 입찰자이면 isTopBidder가 true이다")
    void getMyBids_isTopBidder() {
        Auction auction = createAuction("A-001", AuctionStatus.LIVE, 720000L, 5);
        Bid myBid = Bid.builder().bidId(1L).auctionId("A-001").bidderId(42L).amount(720000L).build();

        given(bidRepository.findDistinctAuctionIdsByBidderId(42L)).willReturn(List.of("A-001"));
        given(auctionRepository.findById("A-001")).willReturn(Optional.of(auction));
        given(bidRepository.findTopByAuctionIdAndBidderId("A-001", 42L)).willReturn(Optional.of(myBid));
        given(bidRepository.findTopByAuctionId("A-001")).willReturn(Optional.of(myBid));

        Page<MyBidResult> result = bidService.getMyBids(42L, null, 0, 20);

        assertThat(result.getContent().get(0).isTopBidder()).isTrue();
    }

    @Test
    @DisplayName("status 필터로 LIVE 경매만 조회한다")
    void getMyBids_filterByStatus() {
        Auction liveAuction = createAuction("A-001", AuctionStatus.LIVE, 500000L, 3);
        Auction endedAuction = createAuction("A-002", AuctionStatus.ENDED, 700000L, 5);

        given(bidRepository.findDistinctAuctionIdsByBidderId(42L)).willReturn(List.of("A-001", "A-002"));
        given(auctionRepository.findById("A-001")).willReturn(Optional.of(liveAuction));
        given(auctionRepository.findById("A-002")).willReturn(Optional.of(endedAuction));
        given(bidRepository.findTopByAuctionIdAndBidderId("A-001", 42L))
                .willReturn(Optional.of(Bid.builder().bidId(1L).auctionId("A-001").bidderId(42L).amount(500000L).build()));
        given(bidRepository.findTopByAuctionId("A-001"))
                .willReturn(Optional.of(Bid.builder().bidId(1L).auctionId("A-001").bidderId(42L).amount(500000L).build()));

        Page<MyBidResult> result = bidService.getMyBids(42L, AuctionStatus.LIVE, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).auctionId()).isEqualTo("A-001");
    }

    @Test
    @DisplayName("입찰한 경매가 없으면 빈 목록을 반환한다")
    void getMyBids_empty() {
        given(bidRepository.findDistinctAuctionIdsByBidderId(42L)).willReturn(List.of());

        Page<MyBidResult> result = bidService.getMyBids(42L, null, 0, 20);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
