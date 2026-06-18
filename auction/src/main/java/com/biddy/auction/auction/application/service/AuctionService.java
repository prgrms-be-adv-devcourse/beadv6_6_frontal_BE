package com.biddy.auction.auction.application.service;

import com.biddy.auction.auction.application.dto.AuctionFeedQuery;
import com.biddy.auction.auction.application.dto.AuctionFeedResult;
import com.biddy.auction.auction.application.usecase.AuctionUseCase;
import com.biddy.auction.auction.domain.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 경매 UseCase 구현체.
 *
 * <p>정렬 전략을 해석하고 Repository에 위임하여 결과를 반환한다.
 * 읽기 전용 트랜잭션으로 실행되어 DB 커넥션 최적화에 기여한다.</p>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuctionService implements AuctionUseCase {

    private final AuctionRepository auctionRepository;

    @Override
    public Page<AuctionFeedResult> getAuctionFeed(AuctionFeedQuery query) {
        Sort sort = resolveSort(query.sort());
        Pageable pageable = PageRequest.of(query.page(), query.size(), sort);

        return auctionRepository.findByFilters(query.status(), query.category(), pageable)
                .map(AuctionFeedResult::from);
    }

    /**
     * 정렬 파라미터 문자열을 Spring Sort 객체로 변환한다.
     * - "ending" → 마감임박순 (endsAt ASC)
     * - "price"  → 높은 가격순 (currentBid DESC)
     * - "latest" 또는 null → 최신순 (createdAt DESC, 기본값)
     */
    private Sort resolveSort(String sort) {
        if (sort == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sort) {
            case "ending" -> Sort.by(Sort.Direction.ASC, "endsAt");
            case "price" -> Sort.by(Sort.Direction.DESC, "currentBid");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}
