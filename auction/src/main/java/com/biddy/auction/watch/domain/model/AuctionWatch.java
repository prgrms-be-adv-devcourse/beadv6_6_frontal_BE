package com.biddy.auction.watch.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 관심 경매 도메인 엔티티.
 *
 * <p>회원이 특정 경매를 관심 등록한 기록을 나타낸다.
 * (auction_id, member_id) 유니크 제약으로 중복 등록을 방지한다.</p>
 */
@Entity
@Table(name = "auction_watch", uniqueConstraints = {
        @UniqueConstraint(name = "uk_watch_auction_member", columnNames = {"auction_id", "member_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuctionWatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "watch_id")
    private Long watchId;

    @Column(name = "auction_id", nullable = false, length = 20)
    private String auctionId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
