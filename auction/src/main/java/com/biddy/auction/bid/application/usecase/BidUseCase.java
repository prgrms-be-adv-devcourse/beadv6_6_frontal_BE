package com.biddy.auction.bid.application.usecase;

import com.biddy.auction.bid.application.dto.BidHistoryQuery;
import com.biddy.auction.bid.application.dto.BidHistoryResult;
import org.springframework.data.domain.Page;

/**
 * 입찰 UseCase 인터페이스.
 *
 * <p>입찰 도메인의 비즈니스 행위를 정의한다.
 * Controller는 이 인터페이스에만 의존하고, 구현체(Service)는 모른다.</p>
 */
public interface BidUseCase {

    Page<BidHistoryResult> getBidHistory(BidHistoryQuery query);
}
