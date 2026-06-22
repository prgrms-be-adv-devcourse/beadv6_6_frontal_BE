package com.biddy.auction.auction.application.dto;

import com.biddy.auction.auction.domain.model.AuctionStatus;

/**
 * 경매 피드 조회 조건 DTO.
 *
 * <p>category 필터는 Product Service 책임이므로 Auction에서 제외.
 * Gateway/BFF에서 Product + Auction 응답을 조합할 때 카테고리 필터링 수행.</p>
 *
 * @param status 경매 상태 필터 (null이면 전체, LIVE | ENDED)
 * @param sort   정렬 기준 (ending: 마감임박, latest: 최신, price: 가격순, null: 최신)
 * @param page   페이지 번호 (0부터)
 * @param size   페이지 크기
 */
public record AuctionFeedQuery(
        AuctionStatus status,
        String sort,
        int page,
        int size
) {
}
