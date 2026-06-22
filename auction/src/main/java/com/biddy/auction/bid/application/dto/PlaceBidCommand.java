package com.biddy.auction.bid.application.dto;

/**
 * 입찰 요청 Command DTO.
 *
 * <p>Controller에서 UseCase로 전달되는 입찰 요청 데이터.
 * bidderId는 인증 정보에서 추출되어 전달된다.</p>
 *
 * @param auctionId 입찰 대상 경매 ID
 * @param bidderId  입찰자 회원 ID (인증 정보에서 추출)
 * @param amount    입찰 금액
 */
public record PlaceBidCommand(
        String auctionId,
        Long bidderId,
        Long amount
) {
}
