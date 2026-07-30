package com.biddy.auction.bid.application.usecase;

import com.biddy.auction.auction.domain.model.AuctionStatus;
import com.biddy.auction.bid.application.dto.BidHistoryQuery;
import com.biddy.auction.bid.application.dto.BidHistoryResult;
import com.biddy.auction.bid.application.dto.MyBidResult;
import org.springframework.data.domain.Page;

/** 입찰 내역과 회원별 참여 경매 조회 유스케이스. */
public interface BidQueryUseCase {

    Page<BidHistoryResult> getBidHistory(BidHistoryQuery query);

    Page<MyBidResult> getMyBids(Long bidderId, AuctionStatus status, int page, int size);
}
