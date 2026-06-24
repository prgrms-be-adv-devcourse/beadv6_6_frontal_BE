package com.biddy.auction.watch.infra.persistence;

import com.biddy.auction.watch.domain.model.AuctionWatch;
import com.biddy.auction.watch.domain.repository.WatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * WatchRepository Port 구현체 (Adapter).
 */
@Repository
@RequiredArgsConstructor
public class WatchRepositoryAdapter implements WatchRepository {

    private final WatchJpaRepository watchJpaRepository;

    @Override
    public Optional<AuctionWatch> findByAuctionIdAndMemberId(String auctionId, Long memberId) {
        return watchJpaRepository.findByAuctionIdAndMemberId(auctionId, memberId);
    }

    @Override
    public AuctionWatch save(AuctionWatch watch) {
        return watchJpaRepository.save(watch);
    }

    @Override
    public void delete(AuctionWatch watch) {
        watchJpaRepository.delete(watch);
    }

    @Override
    public Page<AuctionWatch> findByMemberId(Long memberId, Pageable pageable) {
        return watchJpaRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
    }
}
