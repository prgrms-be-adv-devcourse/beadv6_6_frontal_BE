package com.biddy.auction.bid.infra.persistence;

import com.biddy.auction.bid.domain.model.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Bid JPA Repository (Spring Data JPA).
 *
 * <p>Spring Data의 메서드 네이밍 쿼리를 사용하여
 * auctionId 기반 입찰 내역 페이징 조회를 자동 생성한다.</p>
 */
public interface BidJpaRepository extends JpaRepository<Bid, Long> {

    /** 특정 경매의 입찰 내역을 페이징 조회 (정렬은 Pageable로 전달) */
    Page<Bid> findByAuctionId(String auctionId, Pageable pageable);
}
