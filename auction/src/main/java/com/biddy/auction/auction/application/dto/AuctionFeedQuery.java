package com.biddy.auction.auction.application.dto;

import com.biddy.auction.auction.domain.model.AuctionStatus;

/**
 * 경매 피드 조회 조건 DTO.
 *
 * <p>Controller에서 HTTP 파라미터를 이 객체로 변환하여 UseCase에 전달한다.
 * application 레이어 내부 언어이므로 HTTP 관련 어노테이션이 없다.</p>
 *
 * @param status   경매 상태 필터 (null이면 전체, LIVE | ENDED)
 * @param category 카테고리 필터 (null이면 전체)
 * @param sort     정렬 기준 (ending: 마감임박, latest: 최신, price: 가격순, null: 최신)
 * @param page     페이지 번호 (0부터)
 * @param size     페이지 크기
 */
public record AuctionFeedQuery(
        AuctionStatus status,
        String category,
        String sort,
        int page,
        int size
) {
}
