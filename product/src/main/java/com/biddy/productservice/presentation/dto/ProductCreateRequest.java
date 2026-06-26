package com.biddy.productservice.presentation.dto;

import com.biddy.productservice.domain.model.SaleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "상품 생성 정보")
public record ProductCreateRequest(
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

        @Schema(description = "판매 유형",example = "NORMAL")
        @NotNull(message = "판매 유형 설정은 필수입니다.")
        SaleType saleType,

        @Schema(description = "브랜드",example = "나이키")
        String brand,

        // 경매 전용 필드 (saleType=AUCTION일 때만 필요)
        @Schema(description = "경매 시작가", example = "5000")
        BigDecimal startPrice,

        @Schema(description = "최소 입찰 단위", example = "500")
        Integer minIncrement,

        @Schema(description = "경매 시작일시", example = "2026-06-25T14:00:00")
        LocalDateTime startsAt,

        @Schema(description = "경매 종료일시", example = "2026-06-30T14:00:00")
        LocalDateTime endsAt
) {
}
