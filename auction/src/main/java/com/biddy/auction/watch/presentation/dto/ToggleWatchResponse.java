package com.biddy.auction.watch.presentation.dto;

import com.biddy.auction.watch.application.dto.ToggleWatchResult;

/**
 * 관심 경매 토글 API 응답 DTO.
 *
 * @param watching     현재 관심 등록 상태
 * @param watcherCount 갱신된 관심 등록 수
 */
public record ToggleWatchResponse(
        boolean watching,
        Integer watcherCount
) {

    public static ToggleWatchResponse from(ToggleWatchResult result) {
        return new ToggleWatchResponse(result.watching(), result.watcherCount());
    }
}
