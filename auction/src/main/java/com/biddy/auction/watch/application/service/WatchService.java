package com.biddy.auction.watch.application.service;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.auction.domain.repository.AuctionRepository;
import com.biddy.auction.common.exception.BusinessException;
import com.biddy.auction.common.exception.ErrorCode;
import com.biddy.auction.watch.application.dto.MyWatchResult;
import com.biddy.auction.watch.application.dto.ToggleWatchResult;
import com.biddy.auction.watch.application.usecase.WatchUseCase;
import com.biddy.auction.watch.domain.model.AuctionWatch;
import com.biddy.auction.watch.domain.repository.WatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 관심 경매 UseCase 구현체.
 *
 * <p>토글 방식으로 관심 등록/해제를 처리한다.
 * 이미 등록되어 있으면 해제, 없으면 등록한다.
 * Auction의 watcherCount도 함께 갱신한다.</p>
 */
@Service
@RequiredArgsConstructor
public class WatchService implements WatchUseCase {

    private final WatchRepository watchRepository;
    private final AuctionRepository auctionRepository;

    /**
     * 관심 경매를 토글한다.
     *
     * @param auctionId 경매 ID
     * @param memberId  회원 ID
     * @return 토글 결과 (watching 상태, 갱신된 watcherCount)
     */
    @Override
    @Transactional
    public ToggleWatchResult toggleWatch(String auctionId, Long memberId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        Optional<AuctionWatch> existing = watchRepository.findByAuctionIdAndMemberId(auctionId, memberId);

        if (existing.isPresent()) {
            watchRepository.delete(existing.get());
            auction.decrementWatcherCount();
            return new ToggleWatchResult(false, auction.getWatcherCount());
        } else {
            AuctionWatch watch = AuctionWatch.builder()
                    .auctionId(auctionId)
                    .memberId(memberId)
                    .build();
            watchRepository.save(watch);
            auction.incrementWatcherCount();
            return new ToggleWatchResult(true, auction.getWatcherCount());
        }
    }

    /**
     * 내 관심 경매 목록을 페이징 조회한다.
     *
     * <p>관심 등록된 auctionId로 Auction 엔티티를 조회하여
     * 현재 상태(현재가, 입찰 수 등)를 반환한다.</p>
     *
     * @param memberId 회원 ID
     * @param page     페이지 번호
     * @param size     페이지 크기
     * @return 관심 경매 목록 (최신 등록순)
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MyWatchResult> getMyWatches(Long memberId, int page, int size) {
        Page<AuctionWatch> watches = watchRepository.findByMemberId(memberId, PageRequest.of(page, size));

        return watches.map(watch ->
                auctionRepository.findById(watch.getAuctionId())
                        .map(MyWatchResult::from)
                        .orElse(null)
        );
    }
}
