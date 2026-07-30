package com.biddy.auction.bid.presentation;

import com.biddy.auction.bid.application.usecase.BidV2UseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BidV2Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "bid.api-v2.enabled=false")
class BidV2ControllerDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BidV2UseCase bidV2UseCase;

    @Test
    @DisplayName("기능 플래그가 꺼져 있으면 v2 입찰 라우트를 노출하지 않는다")
    void placeBid_flagDisabled_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/v2/auctions/A-001/bids")
                        .header("X-Member-Id", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestId": "4d47e190-0402-4048-bc2c-89dd54343cdc",
                                  "observedSequence": 5,
                                  "maxAcceptableAmount": 550000
                                }
                                """))
                .andExpect(status().isNotFound());

        verifyNoInteractions(bidV2UseCase);
    }
}
