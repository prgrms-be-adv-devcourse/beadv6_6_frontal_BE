package com.biddy.auction.bid.infra.persistence;

import com.biddy.auction.bid.domain.model.Bid;
import com.biddy.auction.bid.domain.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

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
}
