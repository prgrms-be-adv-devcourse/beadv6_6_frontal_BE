package com.biddy.auction.bid.infra.persistence;

import com.biddy.auction.bid.domain.model.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Bid JPA Repository (Spring Data JPA).
 *
 * <p>Spring Data의 메서드 네이밍 쿼리를 사용하여
 * auctionId 기반 입찰 내역 페이징 조회를 자동 생성한다.</p>
 */
public interface BidJpaRepository extends JpaRepository<Bid, Long> {

    /** 특정 경매의 입찰 내역을 페이징 조회 (정렬은 Pageable로 전달) */
    Page<Bid> findByAuctionId(String auctionId, Pageable pageable);

    /** 특정 경매의 최고 금액 입찰을 조회 */
    Optional<Bid> findTopByAuctionIdOrderByAmountDesc(String auctionId);

    /** 특정 입찰자가 참여한 고유 경매 ID 목록 조회 */
    @Query("SELECT DISTINCT b.auctionId FROM Bid b WHERE b.bidderId = :bidderId ORDER BY b.auctionId")
    List<String> findDistinctAuctionIdsByBidderId(@Param("bidderId") Long bidderId);

    /** 특정 입찰자의 특정 경매에서 최고 입찰 조회 */
    Optional<Bid> findTopByAuctionIdAndBidderIdOrderByAmountDesc(String auctionId, Long bidderId);
}
