package com.biddy.auction.auction.infra.kafka;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Product Service에서 발행하는 경매 상품 등록 이벤트 payload.
 *
 * <p>토픽: {@code product.auction.registered}
 * 경매 상품 등록 시 Product Service가 Kafka로 발행하고,
 * Auction Service의 {@code ProductAuctionRegisteredConsumer}가 수신하여 경매를 자동 생성한다.</p>
 *
 * <p>Product Service가 데이터의 주인(Single Source of Truth)이며,
 * Auction Service는 이벤트에서 비정규화 복사만 수행한다.</p>
 *
 * @param productId    상품 ID (멱등성 체크용)
 * @param sellerId     판매자 회원 ID
 * @param name         상품명
 * @param brand        브랜드
 * @param edition      에디션
 * @param category     카테고리
 * @param description  상품 설명
 * @param thumbnailUrl 대표 이미지 URL
 * @param imageUrls    상세 이미지 URL 목록
 * @param startPrice   경매 시작 가격
 * @param minIncrement 최소 입찰 단위
 * @param endsAt       경매 종료 시각
 */
public record ProductAuctionRegisteredPayload(
        String productId,
        Long sellerId,
        String name,
        String brand,
        String edition,
        String category,
        String description,
        String thumbnailUrl,
        List<String> imageUrls,
        Long startPrice,
        Long minIncrement,
        LocalDateTime endsAt
) {
}
