package com.biddy.auction.auction.infra.kafka;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Product Service의 경매 상품 등록 이벤트 Payload.
 *
 * <p>토픽: {@code product.auction.registered}
 * Auction은 productId + 경매 설정만 저장하고,
 * 상품 정보(name, brand 등)는 저장하지 않는다 (도메인 순수성).</p>
 *
 * @param productId    상품 ID (Product Service FK)
 * @param sellerId     판매자 회원 ID
 * @param startPrice   경매 시작 가격
 * @param minIncrement 최소 입찰 단위
 * @param startsAt     경매 시작 시각
 * @param endsAt       경매 종료 시각
 */
public record ProductAuctionRegisteredPayload(
        UUID productId,
        Long sellerId,
        Long startPrice,
        Long minIncrement,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {
}
