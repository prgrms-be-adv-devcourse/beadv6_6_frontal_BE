package com.biddy.auction.auction.application.usecase;

import com.biddy.auction.auction.application.dto.AuctionDetailResult;
import com.biddy.auction.auction.application.dto.AuctionFeedQuery;
import com.biddy.auction.auction.application.dto.AuctionFeedResult;
import com.biddy.auction.auction.application.dto.AuctionResultInfo;
import org.springframework.data.domain.Page;

/**
 * 경매 UseCase 인터페이스.
 *
 * <p>경매 도메인의 비즈니스 행위를 정의한다.
 * Controller는 이 인터페이스에만 의존하고, 구현체(Service)는 모른다.</p>
 */
public interface AuctionUseCase {

    /** 경매 피드 목록 조회 */
    Page<AuctionFeedResult> getAuctionFeed(AuctionFeedQuery query);

    /** 경매 상세 조회 */
    AuctionDetailResult getAuctionDetail(String auctionId);

    /**
     * 낙찰/유찰 결과를 조회한다.
     * LIVE 상태면 AUCTION_STILL_LIVE 예외를 발생시킨다.
     *
     * @param auctionId 경매 ID
     * @return 낙찰(SOLD) 또는 유찰(UNSOLD) 결과
     */
    AuctionResultInfo getAuctionResult(String auctionId);
}
