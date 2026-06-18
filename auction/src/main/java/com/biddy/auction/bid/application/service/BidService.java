package com.biddy.auction.bid.application.service;

import com.biddy.auction.bid.application.dto.BidHistoryQuery;
import com.biddy.auction.bid.application.dto.BidHistoryResult;
import com.biddy.auction.bid.application.usecase.BidUseCase;
import com.biddy.auction.bid.domain.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 입찰 UseCase 구현체.
 *
 * <p>입찰 내역을 최신순(bidAt DESC)으로 정렬하여 페이징 조회한다.
 * 읽기 전용 트랜잭션으로 실행된다.</p>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BidService implements BidUseCase {

    private final BidRepository bidRepository;

    @Override
    public Page<BidHistoryResult> getBidHistory(BidHistoryQuery query) {
        PageRequest pageable = PageRequest.of(query.page(), query.size(),
                Sort.by(Sort.Direction.DESC, "bidAt"));

        return bidRepository.findByAuctionId(query.auctionId(), pageable)
                .map(BidHistoryResult::from);
    }
}
