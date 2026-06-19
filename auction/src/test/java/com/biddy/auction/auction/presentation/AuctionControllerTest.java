package com.biddy.auction.auction.presentation;

import com.biddy.auction.auction.application.dto.AuctionDetailResult;
import com.biddy.auction.auction.application.dto.AuctionFeedResult;
import com.biddy.auction.auction.application.dto.AuctionResultInfo;
import com.biddy.auction.auction.application.usecase.AuctionUseCase;
import com.biddy.auction.auction.domain.model.AuctionStatus;
import com.biddy.auction.common.exception.BusinessException;
import com.biddy.auction.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    @Nested
    @DisplayName("GET /api/v1/auctions - 경매 피드 조회")
    class GetAuctionFeed {

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
        @DisplayName("경매 피드를 정상 조회한다")
        void returnsOkWithContent() throws Exception {
            given(auctionUseCase.getAuctionFeed(any()))
                    .willReturn(new PageImpl<>(List.of(
                            createFeedResult("A-FNF97", "나이키 덩크", 720000L, 6)
                    ), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/v1/auctions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].auctionId").value("A-FNF97"))
                    .andExpect(jsonPath("$.content[0].currentBid").value(720000))
                    .andExpect(jsonPath("$.content[0].seller.collectorId").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("결과가 없으면 빈 배열을 반환한다")
        void empty_returnsEmptyContent() throws Exception {
            given(auctionUseCase.getAuctionFeed(any()))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

            mockMvc.perform(get("/api/v1/auctions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auctions/{auctionId} - 경매 상세 조회")
    class GetAuctionDetail {

        private AuctionDetailResult createDetailResult(String id) {
            return new AuctionDetailResult(
                    id, "나이키 덩크", "Limited", "Nike",
                    "sneakers", "테스트 설명", "https://img.test.com/thumb.jpg",
                    400000L, 20000L, 720000L, 6,
                    LocalDateTime.of(2026, 6, 20, 15, 0),
                    AuctionStatus.LIVE, 88,
                    new AuctionDetailResult.TopBidderInfo(42L, null),
                    false, null
            );
        }

        @Test
        @DisplayName("존재하는 경매를 상세 조회하면 200과 상세 정보를 반환한다")
        void withExistingAuction_returnsDetail() throws Exception {
            given(auctionUseCase.getAuctionDetail("A-001"))
                    .willReturn(createDetailResult("A-001"));

            mockMvc.perform(get("/api/v1/auctions/A-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.auctionId").value("A-001"))
                    .andExpect(jsonPath("$.name").value("나이키 덩크"))
                    .andExpect(jsonPath("$.startPrice").value(400000))
                    .andExpect(jsonPath("$.minIncrement").value(20000))
                    .andExpect(jsonPath("$.currentBid").value(720000))
                    .andExpect(jsonPath("$.bidCount").value(6))
                    .andExpect(jsonPath("$.status").value("LIVE"))
                    .andExpect(jsonPath("$.watcherCount").value(88))
                    .andExpect(jsonPath("$.topBidder.collectorId").value(42))
                    .andExpect(jsonPath("$.isWatching").value(false))
                    .andExpect(jsonPath("$.myHighestBid").isEmpty());
        }

        @Test
        @DisplayName("존재하지 않는 경매를 조회하면 404를 반환한다")
        void withNonExistingAuction_returns404() throws Exception {
            given(auctionUseCase.getAuctionDetail("INVALID"))
                    .willThrow(new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

            mockMvc.perform(get("/api/v1/auctions/INVALID"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("입찰이 없는 경매를 조회하면 topBidder가 null이다")
        void withNoBids_topBidderIsNull() throws Exception {
            AuctionDetailResult result = new AuctionDetailResult(
                    "A-002", "상품", "Edition", "Brand",
                    "sneakers", "설명", null,
                    100000L, 10000L, 0L, 0,
                    LocalDateTime.of(2026, 6, 20, 15, 0),
                    AuctionStatus.LIVE, 0,
                    null, false, null
            );
            given(auctionUseCase.getAuctionDetail("A-002")).willReturn(result);

            mockMvc.perform(get("/api/v1/auctions/A-002"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.topBidder").isEmpty())
                    .andExpect(jsonPath("$.currentBid").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auctions/{auctionId}/result - 낙찰 결과 조회")
    class GetAuctionResult {

        @Test
        @DisplayName("낙찰된 경매 결과를 조회하면 SOLD를 반환한다")
        void soldAuction_returnsSold() throws Exception {
            AuctionResultInfo result = new AuctionResultInfo(
                    "A-001", "SOLD", 42L, 720000L, 7,
                    LocalDateTime.of(2026, 6, 20, 15, 0),
                    LocalDateTime.of(2026, 6, 21, 15, 0)
            );
            given(auctionUseCase.getAuctionResult("A-001")).willReturn(result);

            mockMvc.perform(get("/api/v1/auctions/A-001/result"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.auctionId").value("A-001"))
                    .andExpect(jsonPath("$.type").value("SOLD"))
                    .andExpect(jsonPath("$.winner.collectorId").value(42))
                    .andExpect(jsonPath("$.finalBid").value(720000))
                    .andExpect(jsonPath("$.totalBids").value(7))
                    .andExpect(jsonPath("$.paymentDeadline").exists());
        }

        @Test
        @DisplayName("유찰된 경매 결과를 조회하면 UNSOLD를 반환한다")
        void unsoldAuction_returnsUnsold() throws Exception {
            AuctionResultInfo result = new AuctionResultInfo(
                    "A-002", "UNSOLD", null, null, 0,
                    LocalDateTime.of(2026, 6, 20, 15, 0),
                    null
            );
            given(auctionUseCase.getAuctionResult("A-002")).willReturn(result);

            mockMvc.perform(get("/api/v1/auctions/A-002/result"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("UNSOLD"))
                    .andExpect(jsonPath("$.winner").doesNotExist())
                    .andExpect(jsonPath("$.finalBid").doesNotExist())
                    .andExpect(jsonPath("$.totalBids").value(0));
        }

        @Test
        @DisplayName("LIVE 상태 경매를 조회하면 409를 반환한다")
        void liveAuction_returns409() throws Exception {
            given(auctionUseCase.getAuctionResult("A-003"))
                    .willThrow(new BusinessException(ErrorCode.AUCTION_STILL_LIVE));

            mockMvc.perform(get("/api/v1/auctions/A-003/result"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("존재하지 않는 경매를 조회하면 404를 반환한다")
        void notFound_returns404() throws Exception {
            given(auctionUseCase.getAuctionResult("A-999"))
                    .willThrow(new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

            mockMvc.perform(get("/api/v1/auctions/A-999/result"))
                    .andExpect(status().isNotFound());
        }
    }
}
