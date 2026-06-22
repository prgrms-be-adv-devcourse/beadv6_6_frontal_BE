package com.biddy.auction.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 공통 설정.
 *
 * <p>RedisTemplate의 키/값 직렬화 전략을 설정한다.
 * Key는 String, Value는 JSON으로 직렬화하여
 * Redis CLI에서도 가독성을 확보한다.</p>
 *
 * <p>향후 활용:
 * <ul>
 *   <li>닉네임 캐시: {@code member:{userId}:nickname}</li>
 *   <li>입찰 Rate Limiting: {@code bid:rate:{userId}}</li>
 *   <li>경매 피드 캐시: {@code auction:feed:LIVE:0:20}</li>
 * </ul></p>
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
