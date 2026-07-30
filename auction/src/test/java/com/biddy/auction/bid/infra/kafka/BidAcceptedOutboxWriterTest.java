package com.biddy.auction.bid.infra.kafka;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.bid.domain.model.Bid;
import com.biddy.auction.outbox.domain.OutboxEvent;
import com.biddy.auction.outbox.domain.OutboxEventRepository;
import com.biddy.auction.outbox.domain.OutboxStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BidAcceptedOutboxWriterTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Test
    @DisplayName("DB eventId와 BID_ACCEPTED payload eventId를 동일하게 저장한다")
    void save_writesMatchingEventIdAndSequence() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        BidAcceptedOutboxWriter writer = new BidAcceptedOutboxWriter(outboxEventRepository, objectMapper);
        Auction auction = Auction.builder()
                .auctionId("A-001")
                .currentBid(510000L)
                .minIncrement(10000L)
                .bidCount(6)
                .build();
        UUID requestId = UUID.randomUUID();
        Bid bid = Bid.builder()
                .bidId(101L)
                .auctionId("A-001")
                .bidderId(42L)
                .amount(510000L)
                .sequence(6L)
                .requestId(requestId)
                .build();
        given(outboxEventRepository.save(any(OutboxEvent.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        writer.save(auction, bid);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent event = captor.getValue();
        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getAggregateType()).isEqualTo("BID");
        assertThat(event.getAggregateId()).isEqualTo("A-001");
        assertThat(event.getTopic()).isEqualTo(BidAcceptedOutboxWriter.TOPIC);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(payload.get("eventId").asText()).isEqualTo(event.getEventId().toString());
        assertThat(payload.get("eventType").asText()).isEqualTo("BID_ACCEPTED");
        assertThat(payload.get("requestId").asText()).isEqualTo(requestId.toString());
        assertThat(payload.get("sequence").asLong()).isEqualTo(6L);
        assertThat(payload.get("currentBid").asLong()).isEqualTo(510000L);
        assertThat(payload.get("nextMinimumBid").asLong()).isEqualTo(520000L);
    }
}
