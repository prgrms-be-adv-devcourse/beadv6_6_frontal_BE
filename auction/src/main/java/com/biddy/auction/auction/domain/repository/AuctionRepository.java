package com.biddy.auction.auction.domain.repository;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.auction.domain.model.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 경매 Repository Port (도메인 레이어 인터페이스).
 *
 * <p>도메인이 필요로 하는 데이터 접근 규격을 정의한다.
 * 실제 구현은 infrastructure 레이어의 {@code AuctionRepositoryImpl}이 담당한다.
 * 이를 통해 도메인이 JPA/DB 기술에 의존하지 않는다 (DIP).</p>
 */
public interface AuctionRepository {

    /**
     * 상태 및 카테고리 필터 기반 경매 목록 조회.
     *
     * @param status   경매 상태 필터 (null이면 전체)
     * @param category 카테고리 필터 (null이면 전체)
     * @param pageable 페이징 및 정렬 정보
     * @return 필터링된 경매 페이지
     */
    Page<Auction> findByFilters(AuctionStatus status, String category, Pageable pageable);
}
