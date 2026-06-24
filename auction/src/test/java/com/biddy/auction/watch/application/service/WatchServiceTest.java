package com.biddy.auction.watch.application.service;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.auction.domain.model.AuctionStatus;
import com.biddy.auction.auction.domain.repository.AuctionRepository;
import com.biddy.auction.common.exception.BusinessException;
import com.biddy.auction.common.exception.ErrorCode;
import com.biddy.auction.watch.application.dto.MyWatchResult;
import com.biddy.auction.watch.application.dto.ToggleWatchResult;
import com.biddy.auction.watch.domain.model.AuctionWatch;
import com.biddy.auction.watch.domain.repository.WatchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WatchServiceTest {

    @InjectMocks
    private WatchService watchService;

    @Mock
    private WatchRepository watchRepository;

    @Mock
    private AuctionRepository auctionRepository;

    private Auction createAuction(int watcherCount) {
        return Auction.builder()
                .auctionId("A-001")
                .sellerId(10L)
                .productId(UUID.randomUUID())
                .startPrice(100000L)
                .minIncrement(10000L)
                .watcherCount(watcherCount)
                .status(AuctionStatus.LIVE)
                .endsAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    @Test
    @DisplayName("관심 미등록 상태에서 토글하면 등록된다")
    void toggleWatch_register() {
        Auction auction = createAuction(88);
        given(auctionRepository.findById("A-001")).willReturn(Optional.of(auction));
        given(watchRepository.findByAuctionIdAndMemberId("A-001", 42L)).willReturn(Optional.empty());
        given(watchRepository.save(any())).willReturn(
                AuctionWatch.builder().watchId(1L).auctionId("A-001").memberId(42L).build()
        );

        ToggleWatchResult result = watchService.toggleWatch("A-001", 42L);

        assertThat(result.watching()).isTrue();
        assertThat(result.watcherCount()).isEqualTo(89);
        verify(watchRepository).save(any(AuctionWatch.class));
        verify(watchRepository, never()).delete(any());
    }

    @Test
    @DisplayName("관심 등록 상태에서 토글하면 해제된다")
    void toggleWatch_unregister() {
        Auction auction = createAuction(88);
        AuctionWatch existing = AuctionWatch.builder()
                .watchId(1L).auctionId("A-001").memberId(42L).build();
        given(auctionRepository.findById("A-001")).willReturn(Optional.of(auction));
        given(watchRepository.findByAuctionIdAndMemberId("A-001", 42L)).willReturn(Optional.of(existing));

        ToggleWatchResult result = watchService.toggleWatch("A-001", 42L);

        assertThat(result.watching()).isFalse();
        assertThat(result.watcherCount()).isEqualTo(87);
        verify(watchRepository).delete(existing);
        verify(watchRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 경매에 토글하면 AUCTION_NOT_FOUND")
    void toggleWatch_auctionNotFound() {
        given(auctionRepository.findById("A-999")).willReturn(Optional.empty());

        assertThatThrownBy(() -> watchService.toggleWatch("A-999", 42L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUCTION_NOT_FOUND);
    }

    @Test
    @DisplayName("watcherCount가 0일 때 해제해도 음수가 되지 않는다")
    void toggleWatch_unregister_zeroCount() {
        Auction auction = createAuction(0);
        AuctionWatch existing = AuctionWatch.builder()
                .watchId(1L).auctionId("A-001").memberId(42L).build();
        given(auctionRepository.findById("A-001")).willReturn(Optional.of(auction));
        given(watchRepository.findByAuctionIdAndMemberId("A-001", 42L)).willReturn(Optional.of(existing));

        ToggleWatchResult result = watchService.toggleWatch("A-001", 42L);

        assertThat(result.watching()).isFalse();
        assertThat(result.watcherCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("내 관심 경매 목록을 조회한다")
    void getMyWatches_returnsList() {
        AuctionWatch watch = AuctionWatch.builder()
                .watchId(1L).auctionId("A-001").memberId(42L).build();
        Auction auction = createAuction(88);

        given(watchRepository.findByMemberId(42L, PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(watch), PageRequest.of(0, 20), 1));
        given(auctionRepository.findById("A-001")).willReturn(Optional.of(auction));

        Page<MyWatchResult> result = watchService.getMyWatches(42L, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).auctionId()).isEqualTo("A-001");
    }

    @Test
    @DisplayName("관심 경매가 없으면 빈 목록을 반환한다")
    void getMyWatches_empty() {
        given(watchRepository.findByMemberId(42L, PageRequest.of(0, 20)))
                .willReturn(Page.empty());

        Page<MyWatchResult> result = watchService.getMyWatches(42L, 0, 20);

        assertThat(result.getContent()).isEmpty();
    }
}
