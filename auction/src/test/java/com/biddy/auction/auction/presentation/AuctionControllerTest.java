package com.biddy.auction.auction.presentation;

import com.biddy.auction.auction.application.dto.AuctionFeedResult;
import com.biddy.auction.auction.application.usecase.AuctionUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuctionController.class)
class AuctionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuctionUseCase auctionUseCase;

    private AuctionFeedResult createFeedResult(String id, String name, Long currentBid, Integer bidCount) {
        return new AuctionFeedResult(
                id, name, "TestEdition", "TestBrand",
                currentBid, bidCount,
                LocalDateTime.of(2026, 6, 12, 15, 30, 0),
                88, "https://img.test.com/thumb.jpg",
                new AuctionFeedResult.SellerInfo(1L, "collector01")
        );
    }

    @Test
    @DisplayName("GET /api/v1/auctions - 경매 피드를 정상 조회한다")
    void getAuctionFeed_returnsOkWithContent() throws Exception {
        List<AuctionFeedResult> results = List.of(
                createFeedResult("A-FNF97", "나이키 덩크", 720000L, 6)
        );
        given(auctionUseCase.getAuctionFeed(any()))
                .willReturn(new PageImpl<>(results, PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/auctions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].auctionId").value("A-FNF97"))
                .andExpect(jsonPath("$.content[0].name").value("나이키 덩크"))
                .andExpect(jsonPath("$.content[0].currentBid").value(720000))
                .andExpect(jsonPath("$.content[0].bidCount").value(6))
                .andExpect(jsonPath("$.content[0].watcherCount").value(88))
                .andExpect(jsonPath("$.content[0].seller.collectorId").value(1))
                .andExpect(jsonPath("$.content[0].seller.nickname").value("collector01"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/auctions?status=LIVE - status 파라미터로 필터링한다")
    void getAuctionFeed_withStatusFilter() throws Exception {
        given(auctionUseCase.getAuctionFeed(any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/auctions")
                        .param("status", "LIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/auctions?sort=ending&category=shoes - 복합 필터를 적용한다")
    void getAuctionFeed_withMultipleFilters() throws Exception {
        given(auctionUseCase.getAuctionFeed(any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/auctions")
                        .param("sort", "ending")
                        .param("category", "shoes"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/auctions?page=1&size=10 - 페이지네이션 파라미터가 적용된다")
    void getAuctionFeed_withPagination() throws Exception {
        given(auctionUseCase.getAuctionFeed(any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));

        mockMvc.perform(get("/api/v1/auctions")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @DisplayName("GET /api/v1/auctions - 결과가 없으면 빈 배열을 반환한다")
    void getAuctionFeed_empty_returnsEmptyContent() throws Exception {
        given(auctionUseCase.getAuctionFeed(any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/auctions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
