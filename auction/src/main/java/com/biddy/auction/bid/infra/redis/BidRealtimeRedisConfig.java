package com.biddy.auction.bid.infra.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/** 모든 Pod가 경매별 Redis 채널을 구독하도록 구성한다. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "bid", name = "websocket-source", havingValue = "redis")
public class BidRealtimeRedisConfig {

    public static final String EVENT_CHANNEL_PATTERN = "auction:realtime:*:events";

    @Bean
    RedisMessageListenerContainer bidRealtimeRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            BidRealtimeRedisSubscriber subscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new PatternTopic(EVENT_CHANNEL_PATTERN));
        return container;
    }
}
