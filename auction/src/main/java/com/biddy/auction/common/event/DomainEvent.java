package com.biddy.auction.common.event;

import java.time.LocalDateTime;

/**
 * Kafka 도메인 이벤트 공통 인터페이스.
 *
 * <p>모든 Kafka 이벤트 Payload가 구현해야 하는 공통 메서드를 정의한다.
 * 이벤트 추적, 로깅, 멱등성 처리에 필요한 최소한의 메타데이터를 포함한다.</p>
 *
 * <p>활용:
 * <ul>
 *   <li>{@code eventType()} — 하나의 토픽에서 여러 이벤트 분기 가능</li>
 *   <li>{@code timestamp()} — 이벤트 순서 보장, 디버깅</li>
 *   <li>{@code aggregateId()} — 멱등성 체크 키</li>
 * </ul></p>
 */
public interface DomainEvent {

    /** 이벤트 유형 (예: AUCTION_ENDED, PRODUCT_REGISTERED) */
    String eventType();

    /** 이벤트 발생 시각 */
    LocalDateTime timestamp();

    /** 집합체 ID (이벤트 대상의 고유 식별자) */
    String aggregateId();
}
