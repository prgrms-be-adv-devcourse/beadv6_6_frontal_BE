package com.biddy.auction.bid.infra.persistence;

import com.biddy.auction.bid.domain.model.Bid;
import com.biddy.auction.bid.domain.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * BidRepository Port 구현체 (Adapter).
 *
 * <p>domain 레이어의 {@code BidRepository} 인터페이스를 구현하여
 * JPA를 통한 실제 데이터 접근을 담당한다.</p>
 */
@Repository
@RequiredArgsConstructor
public class BidRepositoryAdapter implements BidRepository {

    private final BidJpaRepository bidJpaRepository;

    @Override
    public Page<Bid> findByAuctionId(String auctionId, Pageable pageable) {
        return bidJpaRepository.findByAuctionId(auctionId, pageable);
    }

    @Override
    public Optional<Bid> findTopByAuctionId(String auctionId) {
        return bidJpaRepository.findTopByAuctionIdOrderByAmountDesc(auctionId);
    }

    @Override
    public Bid save(Bid bid) {
        return bidJpaRepository.save(bid);
    }

    @Override
    public List<String> findDistinctAuctionIdsByBidderId(Long bidderId) {
        return bidJpaRepository.findDistinctAuctionIdsByBidderId(bidderId);
    }

    @Override
    public Optional<Bid> findTopByAuctionIdAndBidderId(String auctionId, Long bidderId) {
        return bidJpaRepository.findTopByAuctionIdAndBidderIdOrderByAmountDesc(auctionId, bidderId);
    }
}
