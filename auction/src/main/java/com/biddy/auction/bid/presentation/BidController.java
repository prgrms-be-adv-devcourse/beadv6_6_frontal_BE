package com.biddy.auction.bid.presentation;

import com.biddy.auction.bid.application.dto.BidHistoryQuery;
import com.biddy.auction.bid.application.dto.PlaceBidCommand;
import com.biddy.auction.bid.application.dto.PlaceBidResult;
import com.biddy.auction.bid.application.usecase.BidUseCase;
import com.biddy.auction.bid.presentation.dto.BidHistoryResponse;
import com.biddy.auction.bid.presentation.dto.PlaceBidRequest;
import com.biddy.auction.bid.presentation.dto.PlaceBidResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 입찰 REST API Controller.
 *
 * <p>경매별 입찰 내역 조회 API를 제공한다.
 * Auction 도메인과 분리된 독립 Controller로, Bid 도메인의 UseCase만 의존한다.
 * 모든 응답은 {@code ResponseEntity}로 감싸 HTTP 상태 코드를 명시적으로 제어한다.</p>
 */
@RestController
@RequestMapping("/api/v1/auctions/{auctionId}/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidUseCase bidUseCase;

    /**
     * 입찰 실행.
     * POST /api/v1/auctions/{auctionId}/bids
     *
     * <p>1차 검증 → 비관적 락 → 최종 검증 → 입찰 저장 → 현재가 갱신.
     * 인증 미구현 상태에서는 bidderId를 헤더로 임시 전달받는다.</p>
     */
    @PostMapping
    public ResponseEntity<PlaceBidResponse> placeBid(
            @PathVariable String auctionId,
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long bidderId,
            @RequestBody PlaceBidRequest request
    ) {
        PlaceBidCommand command = new PlaceBidCommand(auctionId, bidderId, request.amount());
        PlaceBidResult result = bidUseCase.placeBid(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(PlaceBidResponse.from(result));
    }

    /**
     * 입찰 내역 조회 (최신순).
     * GET /api/v1/auctions/{auctionId}/bids?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<BidHistoryResponse>> getBidHistory(
            @PathVariable String auctionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        BidHistoryQuery query = new BidHistoryQuery(auctionId, page, size);
        Page<BidHistoryResponse> response = bidUseCase.getBidHistory(query)
                .map(BidHistoryResponse::from);
        return ResponseEntity.ok(response);
    }
}
