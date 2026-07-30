package com.biddy.auction.bid.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 입찰 개선 기능을 단계적으로 전환하기 위한 기능 플래그.
 *
 * <p>기본값은 현재 운영 동작과 동일하다. 각 신규 경로는 독립적으로 검증한 뒤
 * 설정만 변경해 활성화하고, 장애 시 코드 롤백 없이 기존 경로로 복귀한다.</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bid")
public class BidFeatureProperties {

    private ExecutionMode executionMode = ExecutionMode.OPTIMISTIC;
    private WebSocketSource websocketSource = WebSocketSource.DIRECT;
    private Toggle apiV2 = new Toggle();
    private Toggle redisProjection = new Toggle();

    public enum ExecutionMode {
        OPTIMISTIC,
        PESSIMISTIC
    }

    public enum WebSocketSource {
        DIRECT,
        REDIS
    }

    @Getter
    @Setter
    public static class Toggle {
        private boolean enabled;
    }
}
