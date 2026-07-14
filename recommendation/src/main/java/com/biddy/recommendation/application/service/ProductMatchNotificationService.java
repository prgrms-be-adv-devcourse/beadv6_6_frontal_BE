package com.biddy.recommendation.application.service;

import com.biddy.recommendation.application.dto.ProductDto;
import com.biddy.recommendation.domain.model.MatchNotification;
import com.biddy.recommendation.domain.model.ProductEmbedding;
import com.biddy.recommendation.domain.model.UserInterest;
import com.biddy.recommendation.domain.repository.MatchNotificationRepository;
import com.biddy.recommendation.domain.repository.ProductEmbeddingRepository;
import com.biddy.recommendation.domain.repository.UserInterestRepository;
import com.biddy.recommendation.infra.acl.ProductClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMatchNotificationService {

    // [2026-07-14] 0.5였다가 0.6으로 완화함 — 실측해보니 "레트로 운동화" ↔ "레트로 컨셉 나이키 스니커즈"
    // 거리가 0.5447로, 0.5 기준으로는 매칭 실패했는데 실제로는 관련 있다고 봐야 하는 케이스였음.
    // (참고: 동일 브랜드+모델 0.43 / 완전 무관 상품 0.66~0.73 — 0.5447은 그 사이 "관련 있음" 구간)
    private static final double MATCH_DISTANCE_THRESHOLD = 0.6;

    private final ProductEmbeddingRepository productEmbeddingRepository;
    private final UserInterestRepository userInterestRepository;
    private final MatchNotificationRepository matchNotificationRepository;
    private final ProductClient productClient;

    public void checkAndNotify(Long productId) {
        try {
            doCheckAndNotify(productId);
        } catch (Exception e) {
            log.error("신규 상품 매칭 알림 처리 중 오류가 발생했습니다. productId={}", productId, e);
        }
    }

    public List<MatchNotification> getNotifications(Long memberId) {
        List<MatchNotification> unread = matchNotificationRepository.findUnreadByMemberId(memberId);
        if (!unread.isEmpty()) {
            matchNotificationRepository.markAllAsRead(memberId);
        }
        return unread;
    }

    private void doCheckAndNotify(Long productId) {
        ProductEmbedding productEmbedding = productEmbeddingRepository.findById(productId).orElse(null);
        if (productEmbedding == null) {
            return;
        }

        List<UserInterest> matches =
                userInterestRepository.findMatchesWithin(productEmbedding.getEmbedding(), MATCH_DISTANCE_THRESHOLD);
        if (matches.isEmpty()) {
            return;
        }

        ProductDto product = productClient.getProduct(productId);
        if (product == null) {
            return;
        }

        for (UserInterest interest : matches) {
            String message = "혹시 '%s' 찾고 계셨나요? %s를 방금 다른 사용자가 등록했어요!"
                    .formatted(interest.getKeyword(), product.name());
            matchNotificationRepository.save(MatchNotification.create(interest.getMemberId(), productId, message));
            log.info("매칭 알림 생성: memberId={}, productId={}, keyword={}",
                    interest.getMemberId(), productId, interest.getKeyword());
        }
    }
}
