package com.biddy.auction.auction.application.service;

import com.biddy.auction.auction.application.dto.AuctionFeedQuery;
import com.biddy.auction.auction.application.dto.AuctionFeedResult;
import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.auction.domain.model.AuctionStatus;
import com.biddy.auction.auction.domain.repository.AuctionRepository;
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
class AuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @InjectMocks
    private AuctionService auctionService;

    private Auction createAuction(String id, String name, Long currentBid, Integer bidCount) {
        return Auction.builder()
                .auctionId(id)
                .sellerId(1L)
                .name(name)
                .brand("TestBrand")
                .edition("TestEdition")
                .startPrice(100000L)
                .minIncrement(10000L)
                .currentBid(currentBid)
                .bidCount(bidCount)
                .watcherCount(10)
                .endsAt(LocalDateTime.of(2026, 6, 20, 15, 0))
                .thumbnailUrl("https://img.test.com/thumb.jpg")
                .build();
    }

    @Test
    @DisplayName("필터 없이 조회하면 전체 경매 피드를 반환한다")
    void getAuctionFeed_withNoFilters_returnsAllAuctions() {
        AuctionFeedQuery query = new AuctionFeedQuery(null, null, null, 0, 20);
        List<Auction> auctions = List.of(
                createAuction("A-001", "상품1", 500000L, 3),
                createAuction("A-002", "상품2", 720000L, 6)
        );
        given(auctionRepository.findByFilters(eq(null), eq(null), any(Pageable.class)))
                .willReturn(new PageImpl<>(auctions));

        Page<AuctionFeedResult> result = auctionService.getAuctionFeed(query);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).auctionId()).isEqualTo("A-001");
        assertThat(result.getContent().get(1).currentBid()).isEqualTo(720000L);
    }

    @Test
    @DisplayName("status=LIVE로 필터링하면 LIVE 경매만 조회한다")
    void getAuctionFeed_withStatusFilter_filtersCorrectly() {
        AuctionFeedQuery query = new AuctionFeedQuery(AuctionStatus.LIVE, null, null, 0, 20);
        List<Auction> auctions = List.of(createAuction("A-001", "상품1", 500000L, 3));
        given(auctionRepository.findByFilters(eq(AuctionStatus.LIVE), eq(null), any(Pageable.class)))
                .willReturn(new PageImpl<>(auctions));

        Page<AuctionFeedResult> result = auctionService.getAuctionFeed(query);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("sort=ending이면 endsAt ASC로 정렬한다")
    void getAuctionFeed_withSortEnding_sortsByEndsAtAsc() {
        AuctionFeedQuery query = new AuctionFeedQuery(null, null, "ending", 0, 20);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(auctionRepository.findByFilters(any(), any(), pageableCaptor.capture()))
                .willReturn(Page.empty());

        auctionService.getAuctionFeed(query);

        Sort sort = pageableCaptor.getValue().getSort();
        assertThat(sort.getOrderFor("endsAt")).isNotNull();
        assertThat(sort.getOrderFor("endsAt").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("sort=price이면 currentBid DESC로 정렬한다")
    void getAuctionFeed_withSortPrice_sortsByCurrentBidDesc() {
        AuctionFeedQuery query = new AuctionFeedQuery(null, null, "price", 0, 20);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(auctionRepository.findByFilters(any(), any(), pageableCaptor.capture()))
                .willReturn(Page.empty());

        auctionService.getAuctionFeed(query);

        Sort sort = pageableCaptor.getValue().getSort();
        assertThat(sort.getOrderFor("currentBid")).isNotNull();
        assertThat(sort.getOrderFor("currentBid").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("sort=latest 또는 null이면 createdAt DESC로 정렬한다")
    void getAuctionFeed_withSortLatest_sortsByCreatedAtDesc() {
        AuctionFeedQuery query = new AuctionFeedQuery(null, null, "latest", 0, 20);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(auctionRepository.findByFilters(any(), any(), pageableCaptor.capture()))
                .willReturn(Page.empty());

        auctionService.getAuctionFeed(query);

        Sort sort = pageableCaptor.getValue().getSort();
        assertThat(sort.getOrderFor("createdAt")).isNotNull();
        assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("Auction을 AuctionFeedResult로 정확히 매핑한다")
    void getAuctionFeed_mapsAuctionToResultCorrectly() {
        AuctionFeedQuery query = new AuctionFeedQuery(null, null, null, 0, 20);
        Auction auction = createAuction("A-FNF97", "나이키 덩크", 720000L, 6);
        given(auctionRepository.findByFilters(any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(auction)));

        Page<AuctionFeedResult> result = auctionService.getAuctionFeed(query);

        AuctionFeedResult item = result.getContent().get(0);
        assertThat(item.auctionId()).isEqualTo("A-FNF97");
        assertThat(item.name()).isEqualTo("나이키 덩크");
        assertThat(item.brand()).isEqualTo("TestBrand");
        assertThat(item.edition()).isEqualTo("TestEdition");
        assertThat(item.currentBid()).isEqualTo(720000L);
        assertThat(item.bidCount()).isEqualTo(6);
        assertThat(item.watcherCount()).isEqualTo(10);
        assertThat(item.seller().collectorId()).isEqualTo(1L);
        assertThat(item.seller().nickname()).isNull();
    }

    @Test
    @DisplayName("페이지네이션 파라미터가 정확히 전달된다")
    void getAuctionFeed_passesPageParametersCorrectly() {
        AuctionFeedQuery query = new AuctionFeedQuery(null, null, null, 2, 10);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(auctionRepository.findByFilters(any(), any(), pageableCaptor.capture()))
                .willReturn(Page.empty());

        auctionService.getAuctionFeed(query);

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }
}
