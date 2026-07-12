package com.biddy.recommendation.presentation.controller;

import com.biddy.recommendation.application.dto.ProductDto;
import com.biddy.recommendation.application.service.CartRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api.init}/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final CartRecommendationService cartRecommendationService;

    @GetMapping("/cart")
    @Operation(summary = "장바구니 기반 추천 상품 조회", description = "회원의 장바구니 상품들과 유사한 상품을 추천합니다.")
    public ResponseEntity<List<ProductDto>> recommendByCart(@RequestHeader("X-Member-Id") Long memberId) {
        return ResponseEntity.ok(cartRecommendationService.recommendByCart(memberId));
    }
}
