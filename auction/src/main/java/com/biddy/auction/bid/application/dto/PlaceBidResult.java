package com.biddy.auction.bid.application.dto;

/**
 * 입찰 결과 DTO.
 *
 * <p>입찰 성공 시 반환되는 결과 데이터.
 * 생성된 입찰 ID, 입찰 금액, 갱신된 현재가, 총 입찰 수를 포함한다.</p>
 *
 * @param bidId      생성된 입찰 ID
 * @param amount     입찰 금액
 * @param currentBid 갱신된 현재 최고가
 * @param bidCount   갱신된 총 입찰 수
 */
public record PlaceBidResult(
        Long bidId,
        Long amount,
        Long currentBid,
        Integer bidCount
) {
}
