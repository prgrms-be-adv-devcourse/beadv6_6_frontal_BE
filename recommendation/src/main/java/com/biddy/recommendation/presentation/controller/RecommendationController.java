package com.biddy.recommendation.presentation.controller;

import com.biddy.recommendation.application.dto.InterestRequest;
import com.biddy.recommendation.application.dto.NotificationDto;
import com.biddy.recommendation.application.dto.ProductDto;
import com.biddy.recommendation.application.service.CartRecommendationService;
import com.biddy.recommendation.application.service.ChecklistSuggestionService;
import com.biddy.recommendation.application.service.ImageSearchService;
import com.biddy.recommendation.application.service.ProductMatchNotificationService;
import com.biddy.recommendation.application.service.UserInterestService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("${api.init}/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final CartRecommendationService cartRecommendationService;
    private final ImageSearchService imageSearchService;
    private final ChecklistSuggestionService checklistSuggestionService;
    private final UserInterestService userInterestService;
    private final ProductMatchNotificationService productMatchNotificationService;

    @GetMapping("/cart")
    @Operation(summary = "장바구니 기반 추천 상품 조회", description = "회원의 장바구니 상품들과 유사한 상품을 추천합니다.")
    public ResponseEntity<List<ProductDto>> recommendByCart(@RequestHeader("X-Member-Id") Long memberId) {
        return ResponseEntity.ok(cartRecommendationService.recommendByCart(memberId));
    }

    @PostMapping(value = "/image-search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "이미지 기반 유사 상품 검색", description = "업로드한 이미지를 분석해 유사한 상품을 검색합니다.")
    public ResponseEntity<List<ProductDto>> searchByImage(
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestPart("image") MultipartFile image) {
        return ResponseEntity.ok(imageSearchService.searchByImage(image));
    }

    @GetMapping("/checklist-suggestions")
    @Operation(summary = "상품 등록 체크리스트 질문 추천 (RAG)", description = "카테고리 기반으로 과거 등록 데이터를 참고해 체크리스트 질문을 추천합니다.")
    public ResponseEntity<List<String>> suggestChecklist(
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestParam("category") String category) {
        return ResponseEntity.ok(checklistSuggestionService.suggest(category));
    }

    @PostMapping("/interests")
    @Operation(summary = "관심 검색어 등록", description = "신규 상품 매칭 알림을 받을 관심 검색어를 등록합니다.")
    public ResponseEntity<Void> registerInterest(
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestBody InterestRequest request) {
        userInterestService.registerInterest(memberId, request.keyword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/notifications")
    @Operation(summary = "신규 상품 매칭 알림 조회", description = "등록한 관심 검색어와 매칭된 신규 상품 알림 목록을 조회합니다.")
    public ResponseEntity<List<NotificationDto>> getNotifications(@RequestHeader("X-Member-Id") Long memberId) {
        List<NotificationDto> notifications = productMatchNotificationService.getNotifications(memberId).stream()
                .map(NotificationDto::from)
                .toList();
        return ResponseEntity.ok(notifications);
    }
}
