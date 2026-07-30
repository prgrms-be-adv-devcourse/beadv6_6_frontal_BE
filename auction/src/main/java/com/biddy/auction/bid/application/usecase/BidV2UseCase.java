package com.biddy.auction.bid.application.usecase;

import com.biddy.auction.bid.application.dto.PlaceBidV2Command;
import com.biddy.auction.bid.application.dto.PlaceBidV2Result;

/** 서버 계산과 sequence/멱등성 계약을 사용하는 입찰 API v2. */
public interface BidV2UseCase {

    PlaceBidV2Result placeBid(PlaceBidV2Command command);
}
