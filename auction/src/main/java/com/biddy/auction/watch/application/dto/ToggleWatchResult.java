package com.biddy.auction.watch.application.dto;

/**
 * 관심 경매 토글 결과 DTO.
 *
 * @param watching     현재 관심 등록 상태 (true=등록, false=해제)
 * @param watcherCount 갱신된 관심 등록 수
 */
public record ToggleWatchResult(
        boolean watching,
        Integer watcherCount
) {
}
