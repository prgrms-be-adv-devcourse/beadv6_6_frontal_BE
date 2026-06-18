package com.biddy.auction.bid.domain.repository;

import com.biddy.auction.bid.domain.model.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 입찰 Repository Port (도메인 레이어 인터페이스).
 *
 * <p>입찰 데이터 접근 규격을 정의한다.
 * 실제 구현은 infrastructure 레이어의 {@code BidRepositoryImpl}이 담당한다.</p>
 */
public interface BidRepository {

    /**
     * 특정 경매의 입찰 내역을 페이징 조회한다.
     *
     * @param auctionId 경매 ID
     * @param pageable  페이징 및 정렬 정보 (기본: bidAt DESC)
     * @return 입찰 내역 페이지
     */
    Page<Bid> findByAuctionId(String auctionId, Pageable pageable);
}
