package com.biddy.auction.bid.infra.redis;

import com.biddy.auction.bid.infra.kafka.BidAcceptedEventPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BidRealtimeProjectionRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private BidRealtimeProjectionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new BidRealtimeProjectionRepository(redisTemplate);
    }

    @Test
    void appliesProjectionAndPublishesOnlyWhenSequenceIsNewer() {
        BidAcceptedEventPayload event = event(7L);
        given(executeScript()).willReturn(1L);

        boolean applied = repository.applyIfNewer(event, "serialized-event");

        assertThat(applied).isTrue();
        verify(redisTemplate).execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                org.mockito.ArgumentMatchers.eq(List.of(
                        "auction:realtime:{A-001}",
                        "auction:realtime:{A-001}:events",
                        "auction:realtime:{A-001}:history"
                )),
                org.mockito.ArgumentMatchers.eq("7"),
                org.mockito.ArgumentMatchers.eq(event.eventId().toString()),
                org.mockito.ArgumentMatchers.eq("570000"),
                org.mockito.ArgumentMatchers.eq("580000"),
                org.mockito.ArgumentMatchers.eq("7"),
                org.mockito.ArgumentMatchers.eq("42"),
                org.mockito.ArgumentMatchers.eq(event.timestamp().toString()),
                org.mockito.ArgumentMatchers.eq("serialized-event")
        );
    }

    @Test
    void reportsDuplicateOrReverseSequenceAsNotApplied() {
        given(executeScript()).willReturn(0L);

        assertThat(repository.applyIfNewer(event(6L), "duplicate")).isFalse();
    }

    private Long executeScript() {
        return redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    private BidAcceptedEventPayload event(long sequence) {
        return new BidAcceptedEventPayload(
                UUID.randomUUID(),
                "BID_ACCEPTED",
                LocalDateTime.of(2026, 7, 31, 1, 0),
                "A-001",
                10L,
                UUID.randomUUID(),
                sequence,
                570000L,
                580000L,
                7,
                42L
        );
    }
}
