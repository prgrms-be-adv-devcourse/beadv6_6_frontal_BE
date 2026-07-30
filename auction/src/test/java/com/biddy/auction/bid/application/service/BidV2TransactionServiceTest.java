package com.biddy.auction.bid.application.service;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.auction.domain.model.AuctionStatus;
import com.biddy.auction.auction.domain.repository.AuctionRepository;
import com.biddy.auction.bid.application.dto.PlaceBidV2Command;
import com.biddy.auction.bid.application.dto.PlaceBidV2Result;
import com.biddy.auction.bid.config.BidFeatureProperties;
import com.biddy.auction.bid.domain.model.Bid;
import com.biddy.auction.bid.domain.repository.BidRepository;
import com.biddy.auction.bid.infra.kafka.BidAcceptedOutboxWriter;
import com.biddy.auction.common.exception.BidConflictException;
import com.biddy.auction.common.exception.BusinessException;
import com.biddy.auction.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BidV2TransactionServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BidFeatureProperties bidFeatureProperties;

    @Mock
    private BidAcceptedOutboxWriter bidAcceptedOutboxWriter;

    @InjectMocks
    private BidV2TransactionService transactionService;

    private Auction auction;
    private UUID requestId;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        auction = Auction.builder()
                .auctionId("A-001")
                .sellerId(10L)
                .productId(1L)
                .startPrice(100000L)
                .currentBid(500000L)
                .minIncrement(10000L)
                .bidCount(5)
                .bidSequence(5L)
                .status(AuctionStatus.LIVE)
                .startsAt(LocalDateTime.now().minusHours(1))
                .endsAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    @Test
    @DisplayName("클라이언트 상한이 더 높아도 서버가 정확한 다음 입찰가만 승인한다")
    void executeBidTransaction_calculatesNextAmountOnServer() {
        PlaceBidV2Command command = command(5L, 550000L);
        Bid savedBid = Bid.builder()
                .bidId(101L)
                .auctionId("A-001")
                .bidderId(42L)
                .requestId(requestId)
                .sequence(6L)
                .amount(510000L)
                .build();

        given(auctionRepository.findById("A-001")).willReturn(Optional.of(auction));
        given(bidRepository.findByBidderIdAndRequestId(42L, requestId)).willReturn(Optional.empty());
        given(auctionRepository.save(auction)).willReturn(auction);
        given(bidRepository.save(any(Bid.class))).willReturn(savedBid);

        PlaceBidV2Result result = transactionService.executeBidTransaction(command);

        assertThat(result.amount()).isEqualTo(510000L);
        assertThat(result.currentBid()).isEqualTo(510000L);
        assertThat(result.nextMinimumBid()).isEqualTo(520000L);
        assertThat(result.sequence()).isEqualTo(6L);
        assertThat(result.bidCount()).isEqualTo(6);
        assertThat(result.idempotentReplay()).isFalse();

        ArgumentCaptor<Bid> bidCaptor = ArgumentCaptor.forClass(Bid.class);
        verify(bidRepository).save(bidCaptor.capture());
        assertThat(bidCaptor.getValue().getAmount()).isEqualTo(510000L);
        assertThat(bidCaptor.getValue().getRequestId()).isEqualTo(requestId);
        assertThat(bidCaptor.getValue().getSequence()).isEqualTo(6L);
        verify(bidAcceptedOutboxWriter).save(auction, savedBid);

        InOrder order = inOrder(auctionRepository, bidRepository);
        order.verify(auctionRepository).save(auction);
        order.verify(auctionRepository).flush();
        order.verify(bidRepository).save(any(Bid.class));
        order.verify(auctionRepository).flush();
    }

    @Test
    @DisplayName("동일 requestId 재요청은 기존 승인 결과를 반환하고 다시 저장하지 않는다")
    void executeBidTransaction_duplicateRequest_replaysExistingResult() {
        Bid existingBid = Bid.builder()
                .bidId(101L)
                .auctionId("A-001")
                .bidderId(42L)
                .requestId(requestId)
                .sequence(4L)
                .amount(490000L)
                .build();
        given(auctionRepository.findById("A-001")).willReturn(Optional.of(auction));
        given(bidRepository.findByBidderIdAndRequestId(42L, requestId))
                .willReturn(Optional.of(existingBid));

        PlaceBidV2Result result = transactionService.executeBidTransaction(command(5L, 550000L));

        assertThat(result.bidId()).isEqualTo(101L);
        assertThat(result.sequence()).isEqualTo(4L);
        assertThat(result.currentBid()).isEqualTo(490000L);
        assertThat(result.nextMinimumBid()).isEqualTo(500000L);
        assertThat(result.bidCount()).isEqualTo(4);
        assertThat(result.idempotentReplay()).isTrue();
        verify(auctionRepository, never()).save(any());
        verify(bidRepository, never()).save(any());
    }

    @Test
    @DisplayName("화면에서 본 sequence가 최신값과 다르면 최신 snapshot으로 409 처리한다")
    void executeBidTransaction_staleSequence_returnsLatestSnapshot() {
        given(auctionRepository.findById("A-001")).willReturn(Optional.of(auction));
        given(bidRepository.findByBidderIdAndRequestId(42L, requestId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.executeBidTransaction(command(4L, 550000L)))
                .isInstanceOfSatisfying(BidConflictException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BID_STALE_STATE);
                    assertThat(exception.getSequence()).isEqualTo(5L);
                    assertThat(exception.getCurrentBid()).isEqualTo(500000L);
                    assertThat(exception.getNextMinimumBid()).isEqualTo(510000L);
                    assertThat(exception.isRetryable()).isTrue();
                });

        verify(auctionRepository, never()).save(any());
        verify(bidRepository, never()).save(any());
    }

    @Test
    @DisplayName("비관적 모드에서는 v2 입찰도 Auction 행 잠금을 획득한다")
    void executeBidTransaction_pessimisticMode_usesRowLock() {
        given(bidFeatureProperties.getExecutionMode())
                .willReturn(BidFeatureProperties.ExecutionMode.PESSIMISTIC);
        given(auctionRepository.findByIdForUpdate("A-001")).willReturn(Optional.of(auction));
        given(bidRepository.findByBidderIdAndRequestId(42L, requestId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.executeBidTransaction(command(4L, 550000L)))
                .isInstanceOfSatisfying(BidConflictException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BID_STALE_STATE));

        verify(auctionRepository).findByIdForUpdate("A-001");
        verify(auctionRepository, never()).findById("A-001");
    }

    @Test
    @DisplayName("서버 다음 입찰가가 사용자 상한보다 높으면 저장하지 않는다")
    void executeBidTransaction_priceChanged_rejectsAboveMaximum() {
        given(auctionRepository.findById("A-001")).willReturn(Optional.of(auction));
        given(bidRepository.findByBidderIdAndRequestId(42L, requestId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.executeBidTransaction(command(5L, 509999L)))
                .isInstanceOfSatisfying(BidConflictException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BID_PRICE_CHANGED);
                    assertThat(exception.getSequence()).isEqualTo(5L);
                    assertThat(exception.getNextMinimumBid()).isEqualTo(510000L);
                });

        verify(auctionRepository, never()).save(any());
        verify(bidRepository, never()).save(any());
    }

    @Test
    @DisplayName("다른 경매에 사용한 requestId는 재사용할 수 없다")
    void executeBidTransaction_requestIdReusedForOtherAuction_rejected() {
        Bid existingBid = Bid.builder()
                .bidId(101L)
                .auctionId("A-OTHER")
                .bidderId(42L)
                .requestId(requestId)
                .sequence(1L)
                .amount(100000L)
                .build();
        given(auctionRepository.findById("A-001")).willReturn(Optional.of(auction));
        given(bidRepository.findByBidderIdAndRequestId(42L, requestId))
                .willReturn(Optional.of(existingBid));

        assertThatThrownBy(() -> transactionService.executeBidTransaction(command(5L, 550000L)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.BID_REQUEST_ID_REUSED);

        verify(auctionRepository, never()).save(any());
        verify(bidRepository, never()).save(any());
    }

    private PlaceBidV2Command command(Long observedSequence, Long maxAcceptableAmount) {
        return new PlaceBidV2Command(
                "A-001",
                42L,
                requestId,
                observedSequence,
                maxAcceptableAmount
        );
    }
}
