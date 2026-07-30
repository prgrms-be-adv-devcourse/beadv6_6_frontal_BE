package com.biddy.auction.bid.application.usecase;

import com.biddy.auction.bid.application.dto.PlaceBidCommand;
import com.biddy.auction.bid.application.dto.PlaceBidResult;

/** 서버 계산과 sequence/멱등성 계약을 사용하는 정식 입찰 유스케이스. */
public interface BidUseCase {

    PlaceBidResult placeBid(PlaceBidCommand command);
}
