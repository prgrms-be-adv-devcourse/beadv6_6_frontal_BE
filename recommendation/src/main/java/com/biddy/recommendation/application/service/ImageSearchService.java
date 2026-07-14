package com.biddy.recommendation.application.service;

import com.biddy.recommendation.application.dto.ProductDto;
import com.biddy.recommendation.domain.repository.ProductEmbeddingRepository;
import com.biddy.recommendation.infra.acl.ImageDescription;
import com.biddy.recommendation.infra.acl.OpenAiEmbeddingClient;
import com.biddy.recommendation.infra.acl.OpenAiVisionClient;
import com.biddy.recommendation.infra.acl.ProductClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageSearchService {

    private static final int SEARCH_LIMIT = 5;

    private final OpenAiVisionClient openAiVisionClient;
    private final OpenAiEmbeddingClient openAiEmbeddingClient;
    private final ProductEmbeddingRepository productEmbeddingRepository;
    private final ProductClient productClient;

    public List<ProductDto> searchByImage(MultipartFile image) {
        try {
            return doSearch(image);
        } catch (Exception e) {
            log.error("이미지 기반 검색 처리 중 오류가 발생하여 빈 목록으로 대체합니다.", e);
            return List.of();
        }
    }

    private List<ProductDto> doSearch(MultipartFile image) throws IOException {
        ImageDescription imageDescription = openAiVisionClient.describeImage(image.getBytes(), image.getContentType());
        log.info("이미지 분석 결과: category={}, description={}", imageDescription.category(), imageDescription.description());
        float[] queryVector = openAiEmbeddingClient.embed(imageDescription.description());

        List<Long> nearestIds = productEmbeddingRepository.findNearestProductIds(
                queryVector, List.of(), List.of(imageDescription.category()), SEARCH_LIMIT);

        return nearestIds.stream()
                .map(productClient::getProduct)
                .filter(Objects::nonNull)
                .toList();
    }
}
