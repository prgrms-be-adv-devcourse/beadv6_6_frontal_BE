package com.biddy.auction.bid.infra.redis;

import com.biddy.auction.auction.infra.websocket.AuctionWebSocketPublisher;
import com.biddy.auction.bid.infra.kafka.BidAcceptedEventPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

/** Redis Pub/Sub 이벤트를 현재 Pod에 연결된 WebSocket 세션으로 전달한다. */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "bid", name = "websocket-source", havingValue = "redis")
public class BidRealtimeRedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final AuctionWebSocketPublisher webSocketPublisher;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String serializedEvent = RedisSerializer.string().deserialize(message.getBody());
        if (serializedEvent == null) {
            log.warn("빈 Redis 입찰 이벤트를 무시합니다.");
            return;
        }

        try {
            BidAcceptedEventPayload event = objectMapper.readValue(
                    serializedEvent,
                    BidAcceptedEventPayload.class
            );
            webSocketPublisher.publishBidFromBroker(event);
        } catch (JsonProcessingException exception) {
            log.error("Redis 입찰 이벤트 역직렬화 실패", exception);
        }
    }
}
