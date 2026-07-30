package com.biddy.auction.bid.presentation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 기존 프론트엔드를 위한 v1 입찰 요청 계약. */
public record PlaceBidV1Request(
        @NotNull(message = "입찰 금액은 필수입니다")
        @Positive(message = "입찰 금액은 0보다 커야 합니다")
        Long amount
) {
}
