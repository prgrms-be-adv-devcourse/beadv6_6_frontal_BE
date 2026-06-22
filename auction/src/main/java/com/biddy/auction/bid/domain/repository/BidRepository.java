package com.biddy.auction.bid.domain.repository;

import com.biddy.auction.bid.domain.model.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

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

    /**
     * 특정 경매의 최고 입찰을 조회한다.
     *
     * @param auctionId 경매 ID
     * @return 최고 금액 입찰 (없으면 empty)
     */
    Optional<Bid> findTopByAuctionId(String auctionId);

    /**
     * 입찰을 저장한다.
     *
     * @param bid 입찰 엔티티
     * @return 저장된 입찰 엔티티 (ID 포함)
     */
    Bid save(Bid bid);

    /**
     * 특정 입찰자가 참여한 고유 경매 ID 목록을 조회한다.
     *
     * @param bidderId 입찰자 회원 ID
     * @return 입찰한 경매 ID 목록 (중복 제거)
     */
    List<String> findDistinctAuctionIdsByBidderId(Long bidderId);

    /**
     * 특정 입찰자의 특정 경매에서 최고 입찰을 조회한다.
     *
     * @param auctionId 경매 ID
     * @param bidderId  입찰자 ID
     * @return 해당 경매에서의 최고 입찰 (없으면 empty)
     */
    Optional<Bid> findTopByAuctionIdAndBidderId(String auctionId, Long bidderId);
}
