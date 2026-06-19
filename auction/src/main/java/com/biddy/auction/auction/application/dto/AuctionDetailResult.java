package com.biddy.auction.auction.application.dto;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.auction.domain.model.AuctionStatus;
import com.biddy.auction.bid.domain.model.Bid;

import java.time.LocalDateTime;

/**
 * 경매 상세 조회 결과 DTO.
 *
 * <p>경매 상세 페이지에 필요한 모든 정보를 담는다.
 * 상품 정보, 가격, 통계, 최고 입찰자, 사용자별 상태(관심/내 최고입찰)를 포함한다.</p>
 */
public record AuctionDetailResult(
        String auctionId,
        String name,
        String edition,
        String brand,
        String category,
        String description,
        String thumbnailUrl,
        Long startPrice,
        Long minIncrement,
        Long currentBid,
        Integer bidCount,
        LocalDateTime endsAt,
        AuctionStatus status,
        Integer watcherCount,
        TopBidderInfo topBidder,
        boolean isWatching,
        Long myHighestBid
) {

    /** 최고 입찰자 정보 */
    public record TopBidderInfo(Long collectorId, String nickname) {
    }

    /**
     * Auction 엔티티 + 최고 입찰 정보로 결과 DTO를 생성한다.
     *
     * @param auction 경매 엔티티
     * @param topBid  최고 입찰 (없으면 null)
     * @return 경매 상세 결과
     */
    public static AuctionDetailResult from(Auction auction, Bid topBid) {
        TopBidderInfo topBidder = topBid != null
                ? new TopBidderInfo(topBid.getBidderId(), null)
                : null;

        return new AuctionDetailResult(
                auction.getAuctionId(),
                auction.getName(),
                auction.getEdition(),
                auction.getBrand(),
                auction.getCategory(),
                auction.getDescription(),
                auction.getThumbnailUrl(),
                auction.getStartPrice(),
                auction.getMinIncrement(),
                auction.getCurrentBid(),
                auction.getBidCount(),
                auction.getEndsAt(),
                auction.getStatus(),
                auction.getWatcherCount(),
                topBidder,
                false,  // TODO: 인증 연동 후 사용자별 관심 여부 조회
                null    // TODO: 인증 연동 후 사용자별 최고 입찰 조회
        );
    }
}
