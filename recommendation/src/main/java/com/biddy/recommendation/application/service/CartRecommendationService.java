package com.biddy.recommendation.application.service;

import com.biddy.recommendation.application.dto.CartItemDto;
import com.biddy.recommendation.application.dto.ProductDto;
import com.biddy.recommendation.domain.model.ProductEmbedding;
import com.biddy.recommendation.domain.repository.ProductEmbeddingRepository;
import com.biddy.recommendation.infra.acl.OrderClient;
import com.biddy.recommendation.infra.acl.ProductClient;
import com.biddy.recommendation.util.VectorTextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartRecommendationService {

    private static final int RECOMMEND_LIMIT = 5;

    private final OrderClient orderClient;
    private final ProductClient productClient;
    private final ProductEmbeddingRepository productEmbeddingRepository;
    // [2026-07-15] 온디맨드 임베딩 생성(아래 doRecommend 주석 참고) 제거하면서 당장은 미사용.
    // 정밀도 문제로 (a) 재도입하게 되면 다시 필요해짐.
    // private final ProductEmbeddingService productEmbeddingService;

    public List<ProductDto> recommendByCart(Long memberId) {
        try {
            return doRecommend(memberId);
        } catch (Exception e) {
            log.error("추천 처리 중 오류가 발생하여 빈 목록으로 대체합니다. memberId={}", memberId, e);
            return List.of();
        }
    }

    private List<ProductDto> doRecommend(Long memberId) {
        List<CartItemDto> cartItems = orderClient.getCartItems(memberId);
        if (cartItems.isEmpty()) {
            return List.of();
        }

        List<Long> cartProductIds = cartItems.stream()
                .map(CartItemDto::productId)
                .distinct()
                .toList();

        // [2026-07-15] 정밀도(추천 유사도)를 높이려고 여기서 임베딩이 없는 상품은 그 자리에서 즉시
        // 생성(ensureEmbedding)하는 방식을 같이 썼었으나, "임베딩은 상품 등록/이미지 검색 시점에만
        // 생성한다"는 원칙에 맞춰 일단 제거함 - 등록 시점에 이미 임베딩됐을 것으로 가정하고 없으면 그냥 제외.
        // 실제 운영 환경에서 추천 정밀도가 너무 떨어지면 재도입 검토.
        // 예전 코드: List<ProductEmbedding> cartEmbeddings = cartProductIds.stream()
        //         .map(productEmbeddingService::ensureEmbedding)
        //         .toList();
        List<ProductEmbedding> cartEmbeddings = cartProductIds.stream()
                .map(productEmbeddingRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        List<float[]> vectors = cartEmbeddings.stream()
                .map(ProductEmbedding::getEmbedding)
                .filter(Objects::nonNull)
                .toList();

        if (vectors.isEmpty()) {
            return List.of();
        }

        List<String> categories = cartEmbeddings.stream()
                .map(ProductEmbedding::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        float[] centroid = VectorTextUtils.average(vectors);
        List<Long> nearestIds = productEmbeddingRepository.findNearestProductIds(centroid, cartProductIds, categories, RECOMMEND_LIMIT);

        return nearestIds.stream()
                .map(productClient::getProduct)
                .filter(Objects::nonNull)
                .toList();
    }
}
