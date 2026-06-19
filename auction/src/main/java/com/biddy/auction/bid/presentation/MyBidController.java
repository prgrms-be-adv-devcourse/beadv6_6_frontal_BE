package com.biddy.auction.bid.presentation;

import com.biddy.auction.auction.domain.model.AuctionStatus;
import com.biddy.auction.bid.application.usecase.BidUseCase;
import com.biddy.auction.bid.presentation.dto.MyBidResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 내 입찰 참여 목록 REST API Controller.
 *
 * <p>인증된 사용자가 입찰에 참여한 경매 목록을 조회한다.
 * 각 경매별 내 최고 입찰 금액과 현재 최고 입찰자 여부를 포함한다.</p>
 */
@RestController
@RequestMapping("/api/v1/members/me/bids")
@RequiredArgsConstructor
public class MyBidController {

    private final BidUseCase bidUseCase;

    /**
     * 내 입찰 참여 목록 조회.
     * GET /api/v1/members/me/bids?status=LIVE&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<MyBidResponse>> getMyBids(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long bidderId,
            @RequestParam(required = false) AuctionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<MyBidResponse> response = bidUseCase.getMyBids(bidderId, status, page, size)
                .map(MyBidResponse::from);
        return ResponseEntity.ok(response);
    }
}
