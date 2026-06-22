package com.biddy.productservice.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "상품 생성 정보")
public record ProductCreateRequest(
        @Schema(description = "판매자 ID",example = "33333333-3333-3333-3333-333333333333")
        UUID sellerId,

        @Schema(description = "상품명",example = "2026 한정판 굿즈")
        String name,

        @Schema(description = "상품 설명",example = "2026 한정판 굿즈, 직거래 희망")
        String description,

        @Schema(description = "가격",example = "50000")
        BigDecimal price,

        @Schema(description = "재고",example = "10")
        int stock,

        @Schema(description = "상품 상태",example = "ACTIVE")
        String status,

        @Schema(description = "카테고리",example = "중고 한정판 굿즈")
        String category,

        @Schema(description = "등록자 Id",example = "33333333-3333-3333-3333-333333333333")
        UUID creatorId
) {
}
