package com.biddy.auction.auction.presentation;

import com.biddy.auction.auction.application.dto.AuctionFeedQuery;
import com.biddy.auction.auction.application.usecase.AuctionUseCase;
import com.biddy.auction.auction.domain.model.AuctionStatus;
import com.biddy.auction.auction.presentation.dto.AuctionFeedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 경매 REST API Controller.
 *
 * <p>HTTP 요청을 받아 UseCase 인터페이스에 위임하고,
 * 결과를 API 응답 DTO로 변환하여 반환한다.</p>
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
    public Page<AuctionFeedResponse> getAuctionFeed(
            @RequestParam(required = false) AuctionStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AuctionFeedQuery query = new AuctionFeedQuery(status, category, sort, page, size);
        return auctionUseCase.getAuctionFeed(query)
                .map(AuctionFeedResponse::from);
    }
}
