package com.biddy.auction.bid.presentation;

import com.biddy.auction.auction.domain.model.AuctionStatus;
import com.biddy.auction.bid.application.usecase.BidUseCase;
import com.biddy.auction.bid.presentation.dto.MyBidResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 내 입찰 참여 목록 REST API Controller.
 */
@Tag(name = "내 입찰", description = "내가 입찰에 참여한 경매 목록 조회 API")
@RestController
@RequestMapping("/api/v1/members/me/bids")
@RequiredArgsConstructor
public class MyBidController {

    private final BidUseCase bidUseCase;

    @Operation(summary = "내 입찰 참여 목록 조회", description = "내가 입찰에 참여한 경매 목록을 조회한다. 경매별 내 최고 입찰 금액과 최고 입찰자 여부를 포함한다.")
    @GetMapping
    public ResponseEntity<Page<MyBidResponse>> getMyBids(
            @Parameter(description = "회원 ID (인증 미구현, 헤더로 임시 전달)") @RequestHeader(value = "X-User-Id", defaultValue = "0") Long bidderId,
            @Parameter(description = "경매 상태 필터 (LIVE, ENDED, null이면 전체)") @RequestParam(required = false) AuctionStatus status,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size
    ) {
        Page<MyBidResponse> response = bidUseCase.getMyBids(bidderId, status, page, size)
                .map(MyBidResponse::from);
        return ResponseEntity.ok(response);
    }
}
