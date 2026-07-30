package com.biddy.auction.bid.infra.redis;

import com.biddy.auction.bid.infra.kafka.BidAcceptedEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

/** DB에서 확정한 sequence를 기준으로 최신 입찰 projection을 원자적으로 갱신한다. */
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bid.redis-projection", name = "enabled", havingValue = "true")
public class BidRealtimeProjectionRepository {

    private static final String KEY_PREFIX = "auction:realtime:";
    private static final String EVENT_CHANNEL_SUFFIX = ":events";
    private static final String HISTORY_KEY_SUFFIX = ":history";

    private static final DefaultRedisScript<Long> APPLY_IF_NEWER_SCRIPT = new DefaultRedisScript<>("""
            local currentSequence = redis.call('HGET', KEYS[1], 'sequence')
            if currentSequence and tonumber(ARGV[1]) <= tonumber(currentSequence) then
                return 0
            end
            redis.call('HSET', KEYS[1],
                'eventId', ARGV[2],
                'sequence', ARGV[1],
                'currentBid', ARGV[3],
                'nextMinimumBid', ARGV[4],
                'bidCount', ARGV[5],
                'bidderId', ARGV[6],
                'timestamp', ARGV[7])
            redis.call('ZADD', KEYS[3], ARGV[1], ARGV[8])
            redis.call('ZREMRANGEBYRANK', KEYS[3], 0, -1001)
            redis.call('PUBLISH', KEYS[2], ARGV[8])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * projection 갱신과 Pub/Sub 발행을 한 Lua 실행으로 처리한다.
     * 동일하거나 과거 sequence는 중복/역순 이벤트로 판단해 무시한다.
     */
    public boolean applyIfNewer(BidAcceptedEventPayload event, String serializedEvent) {
        Long applied = redisTemplate.execute(
                APPLY_IF_NEWER_SCRIPT,
                List.of(
                        projectionKey(event.auctionId()),
                        eventChannel(event.auctionId()),
                        historyKey(event.auctionId())
                ),
                String.valueOf(event.sequence()),
                event.eventId().toString(),
                String.valueOf(event.currentBid()),
                String.valueOf(event.nextMinimumBid()),
                String.valueOf(event.bidCount()),
                String.valueOf(event.bidderId()),
                event.timestamp().toString(),
                serializedEvent
        );
        return Long.valueOf(1L).equals(applied);
    }

    static String projectionKey(String auctionId) {
        return KEY_PREFIX + "{" + auctionId + "}";
    }

    static String eventChannel(String auctionId) {
        return projectionKey(auctionId) + EVENT_CHANNEL_SUFFIX;
    }

    static String historyKey(String auctionId) {
        return projectionKey(auctionId) + HISTORY_KEY_SUFFIX;
    }
}
