package com.biddy.auction.bid.presentation;

import com.biddy.auction.bid.application.dto.PlaceBidCommand;
import com.biddy.auction.bid.application.dto.PlaceBidResult;
import com.biddy.auction.bid.application.usecase.BidUseCase;
import com.biddy.auction.common.exception.BidConflictException;
import com.biddy.auction.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BidController.class)
@AutoConfigureMockMvc(addFilters = false)
class BidControllerTest {

    private static final UUID REQUEST_ID = UUID.fromString("4d47e190-0402-4048-bc2c-89dd54343cdc");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BidUseCase bidUseCase;

    @Test
    @DisplayName("신규 입찰은 서버 계산 결과와 함께 201을 반환한다")
    void placeBid_newRequest_returnsCreated() throws Exception {
        given(bidUseCase.placeBid(any())).willReturn(result(false));

        mockMvc.perform(post("/api/v2/auctions/A-001/bids")
                        .header("X-Member-Id", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.sequence").value(6))
                .andExpect(jsonPath("$.amount").value(510000))
                .andExpect(jsonPath("$.nextMinimumBid").value(520000))
                .andExpect(jsonPath("$.idempotentReplay").value(false));

        ArgumentCaptor<PlaceBidCommand> captor = ArgumentCaptor.forClass(PlaceBidCommand.class);
        verify(bidUseCase).placeBid(captor.capture());
        assertThat(captor.getValue()).isEqualTo(
                new PlaceBidCommand("A-001", 42L, REQUEST_ID, 5L, 550000L));
    }

    @Test
    @DisplayName("동일 requestId 멱등 재응답은 200을 반환한다")
    void placeBid_idempotentReplay_returnsOk() throws Exception {
        given(bidUseCase.placeBid(any())).willReturn(result(true));

        mockMvc.perform(post("/api/v2/auctions/A-001/bids")
                        .header("X-Member-Id", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bidId").value(101))
                .andExpect(jsonPath("$.idempotentReplay").value(true));
    }

    @Test
    @DisplayName("stale sequence 충돌은 최신 snapshot과 함께 409를 반환한다")
    void placeBid_staleSequence_returnsConflictSnapshot() throws Exception {
        given(bidUseCase.placeBid(any())).willThrow(new BidConflictException(
                ErrorCode.BID_STALE_STATE,
                7L,
                530000L,
                540000L
        ));

        mockMvc.perform(post("/api/v2/auctions/A-001/bids")
                        .header("X-Member-Id", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("B006"))
                .andExpect(jsonPath("$.sequence").value(7))
                .andExpect(jsonPath("$.currentBid").value(530000))
                .andExpect(jsonPath("$.nextMinimumBid").value(540000))
                .andExpect(jsonPath("$.retryable").value(true));
    }

    @Test
    @DisplayName("필수 요청 필드가 없으면 400 E001을 반환한다")
    void placeBid_missingRequestId_returnsInvalidInput() throws Exception {
        mockMvc.perform(post("/api/v2/auctions/A-001/bids")
                        .header("X-Member-Id", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observedSequence\":5,\"maxAcceptableAmount\":550000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E001"));
    }

    private String validRequest() {
        return """
                {
                  "requestId": "4d47e190-0402-4048-bc2c-89dd54343cdc",
                  "observedSequence": 5,
                  "maxAcceptableAmount": 550000
                }
                """;
    }

    private PlaceBidResult result(boolean replay) {
        return new PlaceBidResult(
                101L,
                REQUEST_ID,
                6L,
                510000L,
                510000L,
                520000L,
                6,
                replay
        );
    }
}
