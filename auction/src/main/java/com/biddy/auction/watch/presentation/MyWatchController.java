package com.biddy.auction.watch.presentation;

import com.biddy.auction.watch.application.usecase.WatchUseCase;
import com.biddy.auction.watch.presentation.dto.MyWatchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 내 관심 경매 목록 REST API Controller.
 *
 * <p>인증된 사용자의 관심 경매 목록을 조회한다.
 * 인증 미구현 상태에서는 X-User-Id 헤더로 회원 ID를 임시 전달받는다.</p>
 */
@RestController
@RequestMapping("/api/v1/members/me/watches")
@RequiredArgsConstructor
public class MyWatchController {

    private final WatchUseCase watchUseCase;

    /**
     * 내 관심 경매 목록 조회.
     * GET /api/v1/members/me/watches?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<MyWatchResponse>> getMyWatches(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<MyWatchResponse> response = watchUseCase.getMyWatches(memberId, page, size)
                .map(MyWatchResponse::from);
        return ResponseEntity.ok(response);
    }
}
