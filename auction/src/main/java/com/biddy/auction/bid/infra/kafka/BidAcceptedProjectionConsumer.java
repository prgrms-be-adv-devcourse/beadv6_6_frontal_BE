package com.biddy.auction.bid.infra.kafka;

import com.biddy.auction.bid.infra.redis.BidRealtimeProjectionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Kafka 입찰 이벤트를 Redis 실시간 projection으로 직렬 반영한다. */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "bid.redis-projection", name = "enabled", havingValue = "true")
public class BidAcceptedProjectionConsumer {

    private final ObjectMapper objectMapper;
    private final BidRealtimeProjectionRepository projectionRepository;

    @KafkaListener(
            topics = BidAcceptedOutboxWriter.TOPIC,
            groupId = "${bid.redis-projection.consumer-group:auction-bid-projection}"
    )
    public void consume(String serializedEvent) {
        BidAcceptedEventPayload event = deserialize(serializedEvent);
        boolean applied = projectionRepository.applyIfNewer(event, serializedEvent);

        if (applied) {
            log.debug("입찰 Redis projection 반영 - auctionId: {}, sequence: {}, eventId: {}",
                    event.auctionId(), event.sequence(), event.eventId());
        } else {
            log.debug("입찰 중복/역순 이벤트 무시 - auctionId: {}, sequence: {}, eventId: {}",
                    event.auctionId(), event.sequence(), event.eventId());
        }
    }

    private BidAcceptedEventPayload deserialize(String serializedEvent) {
        try {
            return objectMapper.readValue(serializedEvent, BidAcceptedEventPayload.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("BID_ACCEPTED 이벤트 역직렬화에 실패했습니다.", exception);
        }
    }
}
