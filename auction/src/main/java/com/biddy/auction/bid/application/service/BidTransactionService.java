package com.biddy.auction.bid.application.service;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.auction.domain.repository.AuctionRepository;
import com.biddy.auction.bid.application.dto.PlaceBidCommand;
import com.biddy.auction.bid.application.dto.PlaceBidResult;
import com.biddy.auction.bid.config.BidFeatureProperties;
import com.biddy.auction.bid.domain.model.Bid;
import com.biddy.auction.bid.domain.repository.BidRepository;
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
import java.util.UUID;

/**
 * 한 번의 입찰 시도를 독립 트랜잭션으로 처리한다.
 *
 * <p>재시도 오케스트레이터와 트랜잭션 빈을 분리하여 매 재시도가 최신 Auction을
 * 다시 읽고 새로운 트랜잭션에서 실행되도록 한다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BidTransactionService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final BidFeatureProperties bidFeatureProperties;

    /**
     * 입찰 저장과 경매 갱신을 하나의 새 트랜잭션으로 실행한다.
     * flush까지 완료해야 성공 결과를 반환하므로 버전 충돌이 응답 이후로 지연되지 않는다.
     */
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED
    )
    public PlaceBidResult executeBidTransaction(PlaceBidCommand command) {
        validateCommand(command);

        Auction auction = findAuctionForBid(command.auctionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        validateBid(auction, command);

        auction.applyBid(command.amount(), command.bidderId());
        auctionRepository.save(auction);

        // Auction version UPDATE를 먼저 flush해 이 시도의 낙관적 락 승패를 확정한다.
        // Bid의 (auction_id, sequence) 고유 제약이 버전 충돌보다 먼저 발생하는 것을 막는다.
        auctionRepository.flush();

        Bid savedBid = bidRepository.save(Bid.builder()
                .auctionId(command.auctionId())
                .bidderId(command.bidderId())
                .amount(command.amount())
                .sequence(auction.getBidSequence())
                .requestId(UUID.randomUUID())
                .build());

        // Bid INSERT까지 같은 트랜잭션에서 확인한다. 이후 실패하면 앞선 Auction UPDATE도 롤백된다.
        auctionRepository.flush();

        log.debug("입찰 트랜잭션 flush 완료 - 경매: {}, 입찰ID: {}, sequence: {}, 금액: {}원",
                command.auctionId(), savedBid.getBidId(), savedBid.getSequence(), command.amount());

        return new PlaceBidResult(
                savedBid.getBidId(),
                savedBid.getAmount(),
                auction.getCurrentBid(),
                auction.getBidCount()
        );
    }

    private void validateCommand(PlaceBidCommand command) {
        if (command == null
                || command.auctionId() == null
                || command.auctionId().isBlank()
                || command.bidderId() == null
                || command.bidderId() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        if (command.amount() == null || command.amount() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_BID_AMOUNT);
        }
    }

    private Optional<Auction> findAuctionForBid(String auctionId) {
        if (bidFeatureProperties.getExecutionMode() == BidFeatureProperties.ExecutionMode.PESSIMISTIC) {
            return auctionRepository.findByIdForUpdate(auctionId);
        }
        return auctionRepository.findById(auctionId);
    }

    private void validateBid(Auction auction, PlaceBidCommand command) {
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

        if (auction.getSellerId().equals(command.bidderId())) {
            throw new BusinessException(ErrorCode.SELF_BID_NOT_ALLOWED);
        }

        Long requiredAmount = auction.getCurrentBid() + auction.getMinIncrement();
        if (command.amount() < requiredAmount) {
            throw new BusinessException(
                    ErrorCode.BID_AMOUNT_TOO_LOW,
                    "최소 입찰 금액: " + requiredAmount + "원"
            );
        }
    }
}
