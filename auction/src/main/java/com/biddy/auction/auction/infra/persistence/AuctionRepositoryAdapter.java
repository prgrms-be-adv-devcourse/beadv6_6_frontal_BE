package com.biddy.auction.auction.infra.persistence;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.auction.domain.model.AuctionStatus;
import com.biddy.auction.auction.domain.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * AuctionRepository Port 구현체 (Adapter).
 *
 * <p>domain 레이어의 {@code AuctionRepository} 인터페이스를 구현하여
 * JPA를 통한 실제 데이터 접근을 담당한다.
 * domain → infra 의존성을 역전(DIP)시키는 핵심 어댑터.</p>
 */
@Repository
@RequiredArgsConstructor
public class AuctionRepositoryAdapter implements AuctionRepository {

    private final AuctionJpaRepository auctionJpaRepository;

    @Override
    public Page<Auction> findByFilters(AuctionStatus status, String category, Pageable pageable) {
        return auctionJpaRepository.findByFilters(status, category, pageable);
    }
}
