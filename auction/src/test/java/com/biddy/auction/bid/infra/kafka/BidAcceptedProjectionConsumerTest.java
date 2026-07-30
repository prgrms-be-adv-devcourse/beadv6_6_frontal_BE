package com.biddy.auction.bid.infra.kafka;

import com.biddy.auction.bid.infra.redis.BidRealtimeProjectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BidAcceptedProjectionConsumerTest {

    @Mock
    private BidRealtimeProjectionRepository projectionRepository;

    private ObjectMapper objectMapper;
    private BidAcceptedProjectionConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        consumer = new BidAcceptedProjectionConsumer(objectMapper, projectionRepository);
    }

    @Test
    void delegatesValidEventToAtomicRedisProjection() throws Exception {
        BidAcceptedEventPayload event = event();
        String serializedEvent = objectMapper.writeValueAsString(event);

        consumer.consume(serializedEvent);

        verify(projectionRepository).applyIfNewer(event, serializedEvent);
    }

    @Test
    void invalidEventFailsSoKafkaCanRetryOrRecover() {
        assertThatThrownBy(() -> consumer.consume("not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("역직렬화");
    }

    private BidAcceptedEventPayload event() {
        return new BidAcceptedEventPayload(
                UUID.randomUUID(), "BID_ACCEPTED", LocalDateTime.of(2026, 7, 31, 1, 10),
                "A-001", 10L, UUID.randomUUID(), 7L,
                570000L, 580000L, 7, 42L
        );
    }
}
