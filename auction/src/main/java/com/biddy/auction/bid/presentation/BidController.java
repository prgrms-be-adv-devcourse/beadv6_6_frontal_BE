package com.biddy.auction.bid.presentation;

import com.biddy.auction.bid.application.dto.BidHistoryQuery;
import com.biddy.auction.bid.application.usecase.BidUseCase;
import com.biddy.auction.bid.presentation.dto.BidHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 입찰 REST API Controller.
 *
 * <p>경매별 입찰 내역 조회 API를 제공한다.
 * Auction 도메인과 분리된 독립 Controller로, Bid 도메인의 UseCase만 의존한다.</p>
 */
@RestController
@RequestMapping("/api/v1/auctions/{auctionId}/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidUseCase bidUseCase;

    /**
     * 입찰 내역 조회 (최신순).
     * GET /api/v1/auctions/{auctionId}/bids?page=0&size=20
     */
    @GetMapping
    public Page<BidHistoryResponse> getBidHistory(
            @PathVariable String auctionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        BidHistoryQuery query = new BidHistoryQuery(auctionId, page, size);
        return bidUseCase.getBidHistory(query)
                .map(BidHistoryResponse::from);
    }
}
