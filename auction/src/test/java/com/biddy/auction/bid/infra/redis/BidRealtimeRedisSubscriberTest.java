package com.biddy.auction.bid.infra.redis;

import com.biddy.auction.auction.infra.websocket.AuctionWebSocketPublisher;
import com.biddy.auction.bid.infra.kafka.BidAcceptedEventPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BidRealtimeRedisSubscriberTest {

    @Mock
    private AuctionWebSocketPublisher webSocketPublisher;

    @Mock
    private Message message;

    private ObjectMapper objectMapper;
    private BidRealtimeRedisSubscriber subscriber;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        subscriber = new BidRealtimeRedisSubscriber(objectMapper, webSocketPublisher);
    }

    @Test
    void fansOutRedisEventToLocalWebSocketSessions() throws Exception {
        BidAcceptedEventPayload event = event();
        given(message.getBody()).willReturn(objectMapper.writeValueAsBytes(event));

        subscriber.onMessage(message, null);

        verify(webSocketPublisher).publishBidFromBroker(event);
    }

    @Test
    void malformedRedisMessageIsNotPublishedToWebSocket() {
        given(message.getBody()).willReturn("not-json".getBytes(UTF_8));

        subscriber.onMessage(message, null);

        verifyNoInteractions(webSocketPublisher);
    }

    private BidAcceptedEventPayload event() {
        return new BidAcceptedEventPayload(
                UUID.randomUUID(), "BID_ACCEPTED", LocalDateTime.of(2026, 7, 31, 1, 20),
                "A-001", 10L, UUID.randomUUID(), 7L,
                570000L, 580000L, 7, 42L
        );
    }
}
