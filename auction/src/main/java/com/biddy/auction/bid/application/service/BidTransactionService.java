package com.biddy.auction.bid.application.service;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.auction.domain.repository.AuctionRepository;
import com.biddy.auction.bid.application.dto.PlaceBidCommand;
import com.biddy.auction.bid.application.dto.PlaceBidResult;
import com.biddy.auction.bid.config.BidFeatureProperties;
import com.biddy.auction.bid.domain.model.Bid;
import com.biddy.auction.bid.domain.repository.BidRepository;
import com.biddy.auction.bid.infra.kafka.BidAcceptedOutboxWriter;
import com.biddy.auction.common.exception.BidConflictException;
import com.biddy.auction.common.exception.BusinessException;
import com.biddy.auction.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/** 서버 계산·sequence·멱등성 기반 입찰 한 건을 독립 트랜잭션에서 처리한다. */
@Service
@RequiredArgsConstructor
@Slf4j
public class BidTransactionService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final BidFeatureProperties bidFeatureProperties;
    private final BidAcceptedOutboxWriter bidAcceptedOutboxWriter;

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED
    )
    public PlaceBidResult executeBidTransaction(PlaceBidCommand command) {
        validateCommand(command);

        Auction auction = findAuctionForBid(command.auctionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        Bid existingBid = bidRepository.findByBidderIdAndRequestId(
                        command.bidderId(), command.requestId())
                .orElse(null);
        if (existingBid != null) {
            return replayExistingBid(command, auction, existingBid);
        }

        validateAuction(auction, command.bidderId());

        long currentSequence = auction.currentBidSequence();
        long nextMinimumBid = calculateNextAmount(auction.getCurrentBid(), auction.getMinIncrement());

        if (command.observedSequence() != currentSequence) {
            throw new BidConflictException(
                    ErrorCode.BID_STALE_STATE,
                    currentSequence,
                    auction.getCurrentBid(),
                    nextMinimumBid
            );
        }

        if (nextMinimumBid > command.maxAcceptableAmount()) {
            throw new BidConflictException(
                    ErrorCode.BID_PRICE_CHANGED,
                    currentSequence,
                    auction.getCurrentBid(),
                    nextMinimumBid
            );
        }

        auction.applyBid(nextMinimumBid, command.bidderId());
        auctionRepository.save(auction);

        // 현재 단계는 기존 낙관적 락 경로를 사용한다. 다음 단계에서 SELECT FOR UPDATE로 전환한다.
        auctionRepository.flush();

        Bid savedBid = bidRepository.save(Bid.builder()
                .auctionId(command.auctionId())
                .bidderId(command.bidderId())
                .amount(nextMinimumBid)
                .sequence(auction.currentBidSequence())
                .requestId(command.requestId())
                .build());
        bidAcceptedOutboxWriter.save(auction, savedBid);
        auctionRepository.flush();

        long followingMinimumBid = calculateNextAmount(nextMinimumBid, auction.getMinIncrement());
        log.debug("입찰 트랜잭션 완료 - 경매: {}, requestId: {}, sequence: {}, 금액: {}원",
                command.auctionId(), command.requestId(), savedBid.getSequence(), savedBid.getAmount());

        return new PlaceBidResult(
                savedBid.getBidId(),
                savedBid.getRequestId(),
                savedBid.getSequence(),
                savedBid.getAmount(),
                auction.getCurrentBid(),
                followingMinimumBid,
                auction.getBidCount(),
                false
        );
    }

    private PlaceBidResult replayExistingBid(
            PlaceBidCommand command,
            Auction auction,
            Bid existingBid
    ) {
        if (!existingBid.getAuctionId().equals(command.auctionId())) {
            throw new BusinessException(ErrorCode.BID_REQUEST_ID_REUSED);
        }

        long nextMinimumBid = calculateNextAmount(existingBid.getAmount(), auction.getMinIncrement());
        int bidCountAtAcceptance;
        try {
            bidCountAtAcceptance = Math.toIntExact(existingBid.getSequence());
        } catch (ArithmeticException exception) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR);
        }

        return new PlaceBidResult(
                existingBid.getBidId(),
                existingBid.getRequestId(),
                existingBid.getSequence(),
                existingBid.getAmount(),
                existingBid.getAmount(),
                nextMinimumBid,
                bidCountAtAcceptance,
                true
        );
    }

    private void validateCommand(PlaceBidCommand command) {
        if (command == null
                || command.auctionId() == null
                || command.auctionId().isBlank()
                || command.bidderId() == null
                || command.bidderId() <= 0
                || command.requestId() == null
                || command.observedSequence() == null
                || command.observedSequence() < 0
                || command.maxAcceptableAmount() == null
                || command.maxAcceptableAmount() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private Optional<Auction> findAuctionForBid(String auctionId) {
        if (bidFeatureProperties.getExecutionMode() == BidFeatureProperties.ExecutionMode.PESSIMISTIC) {
            return auctionRepository.findByIdForUpdate(auctionId);
        }
        return auctionRepository.findById(auctionId);
    }

    private void validateAuction(Auction auction, Long bidderId) {
        LocalDateTime now = LocalDateTime.now();

        if (auction.getStartsAt() != null && now.isBefore(auction.getStartsAt())) {
            throw new BusinessException(ErrorCode.AUCTION_NOT_STARTED);
        }
        if (auction.getEndsAt() == null) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR);
        }
        if (!auction.isLive() || !now.isBefore(auction.getEndsAt())) {
            throw new BusinessException(ErrorCode.AUCTION_ALREADY_ENDED);
        }
        if (auction.getSellerId().equals(bidderId)) {
            throw new BusinessException(ErrorCode.SELF_BID_NOT_ALLOWED);
        }
    }

    private long calculateNextAmount(Long currentBid, Long minIncrement) {
        if (currentBid == null || currentBid < 0 || minIncrement == null || minIncrement <= 0) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR);
        }
        try {
            return Math.addExact(currentBid, minIncrement);
        } catch (ArithmeticException exception) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR);
        }
    }
}
