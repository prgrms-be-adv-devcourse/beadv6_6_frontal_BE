package com.biddy.auction.auction.infra.websocket;

import com.biddy.auction.bid.config.BidFeatureProperties;
import com.biddy.auction.bid.infra.kafka.BidAcceptedEventPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuctionWebSocketPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private BidFeatureProperties properties;
    private AuctionWebSocketPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new BidFeatureProperties();
        publisher = new AuctionWebSocketPublisher(messagingTemplate, properties);
    }

    @Test
    void directModeKeepsLegacyBidPublishing() {
        publisher.publishBid("A-001", 570000L, 7, 42L);

        verify(messagingTemplate).convertAndSend(
                "/topic/auctions/A-001",
                AuctionWebSocketMessage.bid(570000L, 7, 42L)
        );
    }

    @Test
    void redisModeSuppressesOriginPodDirectPublishing() {
        properties.setWebsocketSource(BidFeatureProperties.WebSocketSource.REDIS);

        publisher.publishBid("A-001", 570000L, 7, 42L);

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void brokerEventIncludesSequenceAndEventIdForClientIntegrityChecks() {
        BidAcceptedEventPayload event = event();
        ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);

        publisher.publishBidFromBroker(event);

        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/auctions/A-001"),
                messageCaptor.capture()
        );
        AuctionWebSocketMessage message = (AuctionWebSocketMessage) messageCaptor.getValue();
        assertThat(message.eventId()).isEqualTo(event.eventId());
        assertThat(message.sequence()).isEqualTo(7L);
        assertThat(message.currentBid()).isEqualTo(570000L);
        assertThat(message.nextMinimumBid()).isEqualTo(580000L);
    }

    private BidAcceptedEventPayload event() {
        return new BidAcceptedEventPayload(
                UUID.randomUUID(), "BID_ACCEPTED", LocalDateTime.of(2026, 7, 31, 1, 30),
                "A-001", 10L, UUID.randomUUID(), 7L,
                570000L, 580000L, 7, 42L
        );
    }
}
