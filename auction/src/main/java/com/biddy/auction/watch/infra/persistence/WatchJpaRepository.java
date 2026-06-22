package com.biddy.auction.watch.infra.persistence;

import com.biddy.auction.watch.domain.model.AuctionWatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * AuctionWatch JPA Repository.
 */
public interface WatchJpaRepository extends JpaRepository<AuctionWatch, Long> {

    Optional<AuctionWatch> findByAuctionIdAndMemberId(String auctionId, Long memberId);

    Page<AuctionWatch> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);
}
