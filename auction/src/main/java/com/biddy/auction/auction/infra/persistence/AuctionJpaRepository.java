package com.biddy.auction.auction.infra.persistence;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.auction.domain.model.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Auction JPA Repository (Spring Data JPA).
 *
 * <p>infra 레이어에 위치하며, domain의 {@code AuctionRepository} Port를
 * 구현하는 {@code AuctionRepositoryImpl}에서 내부적으로 사용한다.
 * 도메인 레이어에서 직접 참조하지 않는다.</p>
 */
public interface AuctionJpaRepository extends JpaRepository<Auction, String> {

    /**
     * 동적 필터 기반 경매 조회 JPQL.
     * IS NULL OR 패턴으로 파라미터가 null이면 해당 조건을 무시한다.
     */
    @Query("""
            SELECT a FROM Auction a
            WHERE (:status IS NULL OR a.status = :status)
              AND (:category IS NULL OR a.category = :category)
            """)
    Page<Auction> findByFilters(
            @Param("status") AuctionStatus status,
            @Param("category") String category,
            Pageable pageable
    );
}
