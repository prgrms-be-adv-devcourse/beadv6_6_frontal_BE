package com.biddy.auction.watch.presentation;

import com.biddy.auction.watch.application.dto.ToggleWatchResult;
import com.biddy.auction.watch.application.usecase.WatchUseCase;
import com.biddy.auction.watch.presentation.dto.ToggleWatchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관심 경매 REST API Controller.
 *
 * <p>토글 방식으로 관심 등록/해제를 처리한다.
 * 인증 미구현 상태에서는 X-User-Id 헤더로 회원 ID를 임시 전달받는다.</p>
 */
@RestController
@RequestMapping("/api/v1/auctions/{auctionId}/watch")
@RequiredArgsConstructor
public class WatchController {

    private final WatchUseCase watchUseCase;

    /**
     * 관심 경매 토글 (등록 ↔ 해제).
     * POST /api/v1/auctions/{auctionId}/watch
     */
    @PostMapping
    public ResponseEntity<ToggleWatchResponse> toggleWatch(
            @PathVariable String auctionId,
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long memberId
    ) {
        ToggleWatchResult result = watchUseCase.toggleWatch(auctionId, memberId);
        return ResponseEntity.ok(ToggleWatchResponse.from(result));
    }
}
