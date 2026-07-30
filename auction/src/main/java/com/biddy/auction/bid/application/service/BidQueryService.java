package com.biddy.auction.bid.application.service;

import com.biddy.auction.auction.domain.model.AuctionStatus;
import com.biddy.auction.auction.domain.repository.AuctionRepository;
import com.biddy.auction.bid.application.dto.BidHistoryQuery;
import com.biddy.auction.bid.application.dto.BidHistoryResult;
import com.biddy.auction.bid.application.dto.MyBidResult;
import com.biddy.auction.bid.application.usecase.BidQueryUseCase;
import com.biddy.auction.bid.domain.model.Bid;
import com.biddy.auction.bid.domain.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 입찰 실행과 분리된 읽기 전용 조회 서비스. */
@Service
@RequiredArgsConstructor
public class BidQueryService implements BidQueryUseCase {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<BidHistoryResult> getBidHistory(BidHistoryQuery query) {
        PageRequest pageable = PageRequest.of(
                query.page(), query.size(), Sort.by(Sort.Direction.DESC, "bidAt")
        );
        return bidRepository.findByAuctionId(query.auctionId(), pageable)
                .map(BidHistoryResult::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MyBidResult> getMyBids(Long bidderId, AuctionStatus status, int page, int size) {
        List<String> auctionIds = bidRepository.findDistinctAuctionIdsByBidderId(bidderId);
        List<MyBidResult> results = auctionIds.stream()
                .map(auctionId -> auctionRepository.findById(auctionId).orElse(null))
                .filter(auction -> auction != null)
                .filter(auction -> status == null || auction.getStatus() == status)
                .map(auction -> {
                    Bid myTopBid = bidRepository
                            .findTopByAuctionIdAndBidderId(auction.getAuctionId(), bidderId)
                            .orElse(null);
                    Bid topBid = bidRepository.findTopByAuctionId(auction.getAuctionId()).orElse(null);

                    return new MyBidResult(
                            auction.getAuctionId(),
                            auction.getProductId(),
                            auction.getStatus().name(),
                            auction.getCurrentBid(),
                            auction.getEndsAt(),
                            myTopBid != null ? myTopBid.getAmount() : null,
                            topBid != null && topBid.getBidderId().equals(bidderId),
                            auction.getBidCount()
                    );
                })
                .toList();

        int start = Math.min(page * size, results.size());
        int end = Math.min(start + size, results.size());
        return new PageImpl<>(results.subList(start, end), PageRequest.of(page, size), results.size());
    }
}
