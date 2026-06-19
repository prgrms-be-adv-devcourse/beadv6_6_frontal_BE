package com.biddy.auction.auction.presentation;

import com.biddy.auction.auction.application.dto.AuctionFeedQuery;
import com.biddy.auction.auction.application.usecase.AuctionUseCase;
import com.biddy.auction.auction.domain.model.AuctionStatus;
import com.biddy.auction.auction.presentation.dto.AuctionDetailResponse;
import com.biddy.auction.auction.presentation.dto.AuctionFeedResponse;
import com.biddy.auction.auction.presentation.dto.AuctionResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 경매 REST API Controller.
 *
 * <p>HTTP 요청을 받아 UseCase 인터페이스에 위임하고,
 * 결과를 API 응답 DTO로 변환하여 반환한다.
 * 모든 응답은 {@code ResponseEntity}로 감싸 HTTP 상태 코드를 명시적으로 제어한다.</p>
 */
@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionUseCase auctionUseCase;

    /**
     * 경매 피드 조회.
     * GET /api/v1/auctions?status=LIVE&category=sneakers&sort=ending&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<AuctionFeedResponse>> getAuctionFeed(
            @RequestParam(required = false) AuctionStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AuctionFeedQuery query = new AuctionFeedQuery(status, category, sort, page, size);
        Page<AuctionFeedResponse> response = auctionUseCase.getAuctionFeed(query)
                .map(AuctionFeedResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 경매 상세 조회.
     * GET /api/v1/auctions/{auctionId}
     */
    @GetMapping("/{auctionId}")
    public ResponseEntity<AuctionDetailResponse> getAuctionDetail(@PathVariable String auctionId) {
        AuctionDetailResponse response = AuctionDetailResponse.from(
                auctionUseCase.getAuctionDetail(auctionId));
        return ResponseEntity.ok(response);
    }

    /**
     * 낙찰/유찰 결과 조회.
     * GET /api/v1/auctions/{auctionId}/result
     *
     * <p>종료된 경매만 조회 가능. LIVE 상태면 409 Conflict.</p>
     */
    @GetMapping("/{auctionId}/result")
    public ResponseEntity<AuctionResultResponse> getAuctionResult(@PathVariable String auctionId) {
        AuctionResultResponse response = AuctionResultResponse.from(
                auctionUseCase.getAuctionResult(auctionId));
        return ResponseEntity.ok(response);
    }
}
