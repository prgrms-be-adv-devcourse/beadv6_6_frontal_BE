package com.biddy.auction.bid.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 입찰 도메인 엔티티.
 *
 * <p>하나의 경매에 대한 개별 입찰 기록을 나타낸다.
 * Auction 엔티티와는 auctionId로 느슨하게 연관되며,
 * JPA 연관관계(@ManyToOne) 대신 ID 참조 방식을 사용하여 도메인 간 결합도를 낮춘다.</p>
 *
 * <p>인덱스:
 * <ul>
 *   <li>{@code idx_bid_auction_bid_at} — 입찰 내역 최신순 조회용</li>
 *   <li>{@code idx_bid_auction_amount} — 최고 입찰 금액 조회용</li>
 * </ul></p>
 */
@Entity
@Table(name = "bid", indexes = {
        @Index(name = "idx_bid_auction_bid_at", columnList = "auction_id, bid_at DESC"),
        @Index(name = "idx_bid_auction_amount", columnList = "auction_id, amount DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_id")
    private Long bidId;

    /** 입찰 대상 경매 ID (Auction 도메인 참조, FK 아닌 ID 참조) */
    @Column(name = "auction_id", nullable = false, length = 20)
    private String auctionId;

    /** 입찰자 회원 ID (Member Service FK) */
    @Column(name = "bidder_id", nullable = false)
    private Long bidderId;

    /** 입찰 금액 (currentBid + minIncrement 이상이어야 유효) */
    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "bid_at", nullable = false)
    private LocalDateTime bidAt;

    @PrePersist
    protected void onCreate() {
        this.bidAt = LocalDateTime.now();
    }
}
