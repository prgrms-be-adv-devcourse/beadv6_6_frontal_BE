package com.biddy.auction.watch.presentation;

import com.biddy.auction.watch.application.dto.ToggleWatchResult;
import com.biddy.auction.watch.application.usecase.WatchUseCase;
import com.biddy.auction.watch.presentation.dto.ToggleWatchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관심 경매 REST API Controller.
 */
@Tag(name = "관심 경매", description = "관심 경매 등록/해제 토글 API")
@RestController
@RequestMapping("/api/v1/auctions/{auctionId}/watch")
@RequiredArgsConstructor
public class WatchController {

    private final WatchUseCase watchUseCase;

    @Operation(summary = "관심 경매 토글", description = "관심 등록 상태이면 해제, 미등록이면 등록한다. watcherCount가 함께 갱신된다.")
    @PostMapping
    public ResponseEntity<ToggleWatchResponse> toggleWatch(
            @Parameter(description = "경매 ID") @PathVariable String auctionId,
            @Parameter(description = "회원 ID (인증 미구현, 헤더로 임시 전달)") @RequestHeader(value = "X-User-Id", defaultValue = "0") Long memberId
    ) {
        ToggleWatchResult result = watchUseCase.toggleWatch(auctionId, memberId);
        return ResponseEntity.ok(ToggleWatchResponse.from(result));
    }
}
