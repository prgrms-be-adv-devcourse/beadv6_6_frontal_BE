package com.biddy.auction.auction.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 경매 도메인 엔티티.
 *
 * <p>경매의 핵심 비즈니스 상태를 관리한다.
 * 상품 정보(name, brand, edition 등)는 MSA 간 동기화 비용을 줄이기 위해
 * 경매 생성 시점에 비정규화하여 보관한다.</p>
 *
 * @see AuctionStatus 경매 상태 (LIVE, ENDED)
 */
@Entity
@Table(name = "auction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Auction {

    @Id
    @Column(name = "auction_id", length = 20)
    private String auctionId;

    /** 판매자 회원 ID (Member Service FK) */
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    // -- 상품 정보 (비정규화) --

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "edition", length = 100)
    private String edition;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    // -- 가격 정보 --

    /** 경매 시작 가격 */
    @Column(name = "start_price", nullable = false)
    private Long startPrice;

    /** 최소 입찰 단위 (입찰 금액 >= currentBid + minIncrement) */
    @Column(name = "min_increment", nullable = false)
    private Long minIncrement;

    /** 현재 최고 입찰가 (입찰 시 갱신) */
    @Builder.Default
    @Column(name = "current_bid", nullable = false)
    private Long currentBid = 0L;

    // -- 통계 --

    @Builder.Default
    @Column(name = "bid_count", nullable = false)
    private Integer bidCount = 0;

    @Builder.Default
    @Column(name = "watcher_count", nullable = false)
    private Integer watcherCount = 0;

    // -- 상태 및 시간 --

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AuctionStatus status = AuctionStatus.LIVE;

    /** 경매 종료 시각 (스케줄러가 이 시각 이후 종료 처리) */
    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
