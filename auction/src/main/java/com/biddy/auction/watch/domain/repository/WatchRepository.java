package com.biddy.auction.watch.domain.repository;

import com.biddy.auction.watch.domain.model.AuctionWatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * 관심 경매 Repository Port.
 */
public interface WatchRepository {

    /** 특정 회원의 특정 경매 관심 등록 조회 */
    Optional<AuctionWatch> findByAuctionIdAndMemberId(String auctionId, Long memberId);

    /** 관심 경매 저장 */
    AuctionWatch save(AuctionWatch watch);

    /** 관심 경매 삭제 */
    void delete(AuctionWatch watch);

    /** 특정 회원의 관심 경매 목록을 페이징 조회 (최신 등록순) */
    Page<AuctionWatch> findByMemberId(Long memberId, Pageable pageable);
}
