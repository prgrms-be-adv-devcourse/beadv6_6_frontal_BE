package com.biddy.auction.bid.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class BidFeaturePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void keepsCurrentBehaviorAsDefaults() {
        contextRunner.run(context -> {
            BidFeatureProperties properties = context.getBean(BidFeatureProperties.class);

            assertThat(properties.getExecutionMode())
                    .isEqualTo(BidFeatureProperties.ExecutionMode.OPTIMISTIC);
            assertThat(properties.getWebsocketSource())
                    .isEqualTo(BidFeatureProperties.WebSocketSource.DIRECT);
            assertThat(properties.getApiV2().isEnabled()).isFalse();
            assertThat(properties.getRedisProjection().isEnabled()).isFalse();
        });
    }

    @Test
    void bindsEveryTransitionFlag() {
        contextRunner
                .withPropertyValues(
                        "bid.execution-mode=pessimistic",
                        "bid.websocket-source=redis",
                        "bid.api-v2.enabled=true",
                        "bid.redis-projection.enabled=true"
                )
                .run(context -> {
                    BidFeatureProperties properties = context.getBean(BidFeatureProperties.class);

                    assertThat(properties.getExecutionMode())
                            .isEqualTo(BidFeatureProperties.ExecutionMode.PESSIMISTIC);
                    assertThat(properties.getWebsocketSource())
                            .isEqualTo(BidFeatureProperties.WebSocketSource.REDIS);
                    assertThat(properties.getApiV2().isEnabled()).isTrue();
                    assertThat(properties.getRedisProjection().isEnabled()).isTrue();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(BidFeatureProperties.class)
    static class TestConfiguration {
    }
}
