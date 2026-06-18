package com.biddy.auction.auction.application.usecase;

import com.biddy.auction.auction.application.dto.AuctionFeedQuery;
import com.biddy.auction.auction.application.dto.AuctionFeedResult;
import org.springframework.data.domain.Page;

/**
 * 경매 UseCase 인터페이스.
 *
 * <p>경매 도메인의 비즈니스 행위를 정의한다.
 * Controller는 이 인터페이스에만 의존하고, 구현체(Service)는 모른다.</p>
 */
public interface AuctionUseCase {

    Page<AuctionFeedResult> getAuctionFeed(AuctionFeedQuery query);
}
