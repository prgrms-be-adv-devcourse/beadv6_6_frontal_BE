package com.biddy.auction.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP 설정.
 *
 * <p>클라이언트가 {@code /ws} 엔드포인트로 WebSocket 연결을 수립하고,
 * {@code /topic/auctions/{auctionId}} 채널을 구독하면
 * 입찰/종료 이벤트를 실시간으로 수신한다.</p>
 *
 * <p>STOMP 프로토콜을 사용하여 메시지 라우팅을 단순화하고,
 * SockJS fallback으로 WebSocket 미지원 환경도 대응한다.</p>
 *
 * <p>구독 경로:
 * <ul>
 *   <li>{@code /topic/auctions/{auctionId}} — 경매별 실시간 이벤트 (BID, ENDED)</li>
 * </ul></p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 메시지 브로커 설정.
     * - /topic: 1:N 브로드캐스트 prefix (경매 구독자 전체)
     * - /app: 클라이언트 → 서버 메시지 prefix
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * STOMP 엔드포인트 등록.
     * - /ws: WebSocket 연결 엔드포인트
     * - SockJS fallback 활성화
     * - CORS 허용 (개발 환경)
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 순수 WebSocket (STOMP.js Client용)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        // SockJS fallback (WebSocket 미지원 환경용)
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
