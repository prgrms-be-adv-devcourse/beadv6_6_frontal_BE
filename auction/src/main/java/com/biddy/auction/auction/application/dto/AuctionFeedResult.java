package com.biddy.auction.auction.application.dto;

import com.biddy.auction.auction.domain.model.Auction;

import java.time.LocalDateTime;

/**
 * 경매 피드 조회 결과 DTO.
 *
 * <p>Entity → Result 변환을 통해 도메인 엔티티가 외부로 직접 노출되지 않도록 보호한다.
 * nickname은 현재 null로 반환되며, 추후 Member Service 연동(ACL) 시 채워진다.</p>
 */
public record AuctionFeedResult(
        String auctionId,
        String name,
        String edition,
        String brand,
        Long currentBid,
        Integer bidCount,
        LocalDateTime endsAt,
        Integer watcherCount,
        String thumbnailUrl,
        SellerInfo seller
) {

    /** 판매자 정보 (Member Service 연동 전까지 nickname은 null) */
    public record SellerInfo(Long collectorId, String nickname) {
    }

    /** Auction Entity → AuctionFeedResult 변환 팩토리 메서드 */
    public static AuctionFeedResult from(Auction auction) {
        return new AuctionFeedResult(
                auction.getAuctionId(),
                auction.getName(),
                auction.getEdition(),
                auction.getBrand(),
                auction.getCurrentBid(),
                auction.getBidCount(),
                auction.getEndsAt(),
                auction.getWatcherCount(),
                auction.getThumbnailUrl(),
                new SellerInfo(auction.getSellerId(), null)
        );
    }
}
