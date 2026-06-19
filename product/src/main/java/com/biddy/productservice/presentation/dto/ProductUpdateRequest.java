package com.biddy.productservice.presentation.dto;


import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "상품 수정 정보")
public record ProductUpdateRequest(

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

        @Schema(description = "상표",example = "나이키")
        String brand,

        @Schema(description = "수정자 Id",example = "33333333-3333-3333-3333-333333333333")
        UUID modifierId
) {
}
