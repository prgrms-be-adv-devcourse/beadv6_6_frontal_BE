package com.biddy.auction.watch.application.usecase;

import com.biddy.auction.watch.application.dto.MyWatchResult;
import com.biddy.auction.watch.application.dto.ToggleWatchResult;
import org.springframework.data.domain.Page;

/**
 * 관심 경매 UseCase 인터페이스.
 */
public interface WatchUseCase {

    /**
     * 관심 경매를 토글한다 (등록 ↔ 해제).
     *
     * @param auctionId 경매 ID
     * @param memberId  회원 ID
     * @return 토글 결과 (현재 상태, 관심 수)
     */
    ToggleWatchResult toggleWatch(String auctionId, Long memberId);

    /**
     * 내 관심 경매 목록을 페이징 조회한다.
     *
     * @param memberId 회원 ID
     * @param page     페이지 번호
     * @param size     페이지 크기
     * @return 관심 경매 목록 (최신 등록순)
     */
    Page<MyWatchResult> getMyWatches(Long memberId, int page, int size);
}
