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

@Slf4j
@Service
@RequiredArgsConstructor
public class CartRecommendationService {

    private static final int RECOMMEND_LIMIT = 5;

    private final OrderClient orderClient;
    private final ProductClient productClient;
    private final ProductEmbeddingRepository productEmbeddingRepository;
    private final ProductEmbeddingService productEmbeddingService;

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

        List<ProductEmbedding> cartEmbeddings = cartProductIds.stream()
                .map(productEmbeddingService::ensureEmbedding)
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
