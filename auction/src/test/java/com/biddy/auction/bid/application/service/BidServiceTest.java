package com.biddy.auction.bid.application.service;

import com.biddy.auction.bid.application.dto.BidHistoryQuery;
import com.biddy.auction.bid.application.dto.BidHistoryResult;
import com.biddy.auction.bid.domain.model.Bid;
import com.biddy.auction.bid.domain.repository.BidRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock
    private BidRepository bidRepository;

    @InjectMocks
    private BidService bidService;

    private Bid createBid(Long bidId, String auctionId, Long bidderId, Long amount, LocalDateTime bidAt) {
        return Bid.builder()
                .bidId(bidId)
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(amount)
                .bidAt(bidAt)
                .build();
    }

    @Test
    @DisplayName("auctionId로 입찰 내역을 조회한다")
    void getBidHistory_returnsBidHistory() {
        String auctionId = "A-FNF97";
        BidHistoryQuery query = new BidHistoryQuery(auctionId, 0, 20);
        List<Bid> bids = List.of(
                createBid(101L, auctionId, 42L, 720000L, LocalDateTime.of(2026, 6, 12, 13, 55)),
                createBid(100L, auctionId, 33L, 700000L, LocalDateTime.of(2026, 6, 12, 13, 50))
        );
        given(bidRepository.findByAuctionId(eq(auctionId), any(Pageable.class)))
                .willReturn(new PageImpl<>(bids));

        Page<BidHistoryResult> result = bidService.getBidHistory(query);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).amount()).isEqualTo(720000L);
        assertThat(result.getContent().get(0).bidder().collectorId()).isEqualTo(42L);
        assertThat(result.getContent().get(1).amount()).isEqualTo(700000L);
    }

    @Test
    @DisplayName("bidAt DESC 정렬로 조회한다 (최신순)")
    void getBidHistory_sortsByBidAtDesc() {
        BidHistoryQuery query = new BidHistoryQuery("A-001", 0, 20);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(bidRepository.findByAuctionId(any(), pageableCaptor.capture()))
                .willReturn(Page.empty());

        bidService.getBidHistory(query);

        Sort sort = pageableCaptor.getValue().getSort();
        assertThat(sort.getOrderFor("bidAt")).isNotNull();
        assertThat(sort.getOrderFor("bidAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("Bid를 BidHistoryResult로 정확히 매핑한다")
    void getBidHistory_mapsBidToResultCorrectly() {
        BidHistoryQuery query = new BidHistoryQuery("A-FNF97", 0, 20);
        LocalDateTime bidAt = LocalDateTime.of(2026, 6, 12, 13, 55);
        Bid bid = createBid(101L, "A-FNF97", 42L, 720000L, bidAt);
        given(bidRepository.findByAuctionId(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(bid)));

        Page<BidHistoryResult> result = bidService.getBidHistory(query);

        BidHistoryResult item = result.getContent().get(0);
        assertThat(item.bidder().collectorId()).isEqualTo(42L);
        assertThat(item.bidder().nickname()).isNull();
        assertThat(item.amount()).isEqualTo(720000L);
        assertThat(item.bidAt()).isEqualTo(bidAt);
    }

    @Test
    @DisplayName("페이지네이션 파라미터가 정확히 전달된다")
    void getBidHistory_passesPageParametersCorrectly() {
        BidHistoryQuery query = new BidHistoryQuery("A-001", 1, 10);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(bidRepository.findByAuctionId(any(), pageableCaptor.capture()))
                .willReturn(Page.empty());

        bidService.getBidHistory(query);

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("입찰 내역이 없으면 빈 페이지를 반환한다")
    void getBidHistory_noBids_returnsEmptyPage() {
        BidHistoryQuery query = new BidHistoryQuery("A-EMPTY", 0, 20);
        given(bidRepository.findByAuctionId(any(), any(Pageable.class)))
                .willReturn(Page.empty());

        Page<BidHistoryResult> result = bidService.getBidHistory(query);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
