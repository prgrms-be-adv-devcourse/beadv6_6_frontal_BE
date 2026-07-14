package com.biddy.recommendation.application.service;

import com.biddy.recommendation.application.dto.ProductDto;
import com.biddy.recommendation.domain.repository.ProductEmbeddingRepository;
import com.biddy.recommendation.infra.acl.ChecklistSuggestion;
import com.biddy.recommendation.infra.acl.OpenAiChatClient;
import com.biddy.recommendation.infra.acl.ProductClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChecklistSuggestionService {

    private static final int PAST_PRODUCT_LIMIT = 5;
    private static final List<String> DEFAULT_QUESTIONS = List.of(
            "브랜드는 무엇인가요?",
            "주요 특징이나 사용감을 알려주세요.",
            "구매 시기 또는 사용 기간은 어느 정도인가요?"
    );

    private final ProductEmbeddingRepository productEmbeddingRepository;
    private final ProductClient productClient;
    private final OpenAiChatClient openAiChatClient;

    public List<String> suggest(String category) {
        try {
            return doSuggest(category);
        } catch (Exception e) {
            log.error("체크리스트 추천 처리 중 오류가 발생하여 기본 질문으로 대체합니다. category={}", category, e);
            return DEFAULT_QUESTIONS;
        }
    }

    private List<String> doSuggest(String category) {
        List<Long> pastProductIds = productEmbeddingRepository.findProductIdsByCategory(category, PAST_PRODUCT_LIMIT);

        List<Map<String, String>> pastAnswers = pastProductIds.stream()
                .map(productClient::getProduct)
                .filter(Objects::nonNull)
                .map(ProductDto::checklistAnswers)
                .filter(answers -> answers != null && !answers.isEmpty())
                .toList();

        String prompt = pastAnswers.isEmpty()
                ? buildGenerationOnlyPrompt(category)
                : buildRetrievalAugmentedPrompt(category, pastAnswers);

        log.info("체크리스트 추천 프롬프트 (category={}, 참고데이터 {}건): {}", category, pastAnswers.size(), prompt);

        ChecklistSuggestion suggestion = openAiChatClient.generateChecklistSuggestion(prompt);
        if (suggestion.questions() == null || suggestion.questions().isEmpty()) {
            return DEFAULT_QUESTIONS;
        }
        return suggestion.questions();
    }

    private String buildRetrievalAugmentedPrompt(String category, List<Map<String, String>> pastAnswers) {
        String examples = pastAnswers.stream()
                .map(Object::toString)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        return """
                당신은 중고거래 플랫폼의 상품 등록 도우미입니다.
                카테고리 "%s"에 과거 등록된 상품들의 체크리스트 답변 예시는 다음과 같습니다:
                %s

                위 예시들을 참고해서, "%s" 카테고리 상품을 등록하는 판매자에게 물어보면 좋을
                체크리스트 질문 3개를 한국어로 만들어줘.
                아래 JSON 형식으로만 답변해줘. 다른 텍스트는 붙이지 마.
                {"questions": ["질문1", "질문2", "질문3"]}
                """.formatted(category, examples, category);
    }

    private String buildGenerationOnlyPrompt(String category) {
        return """
                당신은 중고거래 플랫폼의 상품 등록 도우미입니다.
                "%s" 카테고리는 아직 참고할 과거 등록 데이터가 없습니다.
                이 카테고리 특성을 고려해서, 판매자에게 물어보면 좋을 체크리스트 질문 3개를
                한국어로 만들어줘.
                아래 JSON 형식으로만 답변해줘. 다른 텍스트는 붙이지 마.
                {"questions": ["질문1", "질문2", "질문3"]}
                """.formatted(category);
    }
}
