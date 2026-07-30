package com.biddy.auction.bid.presentation;

import com.biddy.auction.bid.application.dto.BidHistoryResult;
import com.biddy.auction.bid.application.usecase.BidQueryUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BidHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class BidHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BidQueryUseCase bidQueryUseCase;

    @Test
    @DisplayName("GET /api/v1/auctions/{auctionId}/bids - 입찰 내역을 정상 조회한다")
    void getBidHistory_returnsOkWithContent() throws Exception {
        List<BidHistoryResult> results = List.of(
                new BidHistoryResult(
                        new BidHistoryResult.BidderInfo(42L, "collector01"),
                        720000L,
                        LocalDateTime.of(2026, 6, 12, 13, 55, 0)
                )
        );
        given(bidQueryUseCase.getBidHistory(any()))
                .willReturn(new PageImpl<>(results, PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/auctions/A-FNF97/bids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].bidder.collectorId").value(42))
                .andExpect(jsonPath("$.content[0].bidder.nickname").value("collector01"))
                .andExpect(jsonPath("$.content[0].amount").value(720000))
                .andExpect(jsonPath("$.content[0].bidAt").exists())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/auctions/{auctionId}/bids?page=1&size=10 - 페이지네이션이 적용된다")
    void getBidHistory_withPagination() throws Exception {
        given(bidQueryUseCase.getBidHistory(any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));

        mockMvc.perform(get("/api/v1/auctions/A-001/bids")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @DisplayName("GET /api/v1/auctions/{auctionId}/bids - 입찰 내역이 없으면 빈 배열을 반환한다")
    void getBidHistory_empty_returnsEmptyContent() throws Exception {
        given(bidQueryUseCase.getBidHistory(any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/auctions/A-EMPTY/bids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("기존 v1 입찰 POST는 더 이상 노출하지 않는다")
    void placeBid_legacyV1_returnsMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/api/v1/auctions/A-001/bids")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":520000}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("E004"));
    }

    @Test
    @DisplayName("GET 입찰 내역의 음수 페이지는 400 E001")
    void getBidHistory_negativePage_returnsInvalidInput() throws Exception {
        mockMvc.perform(get("/api/v1/auctions/A-001/bids")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E001"));
    }
}
