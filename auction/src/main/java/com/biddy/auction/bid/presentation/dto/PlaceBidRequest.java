package com.biddy.auction.bid.presentation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

/** 서버 계산 입찰 요청. */
public record PlaceBidRequest(
        @NotNull(message = "입찰 요청 ID는 필수입니다")
        UUID requestId,

        @NotNull(message = "확인한 입찰 순서는 필수입니다")
        @PositiveOrZero(message = "확인한 입찰 순서는 0 이상이어야 합니다")
        Long observedSequence,

        @NotNull(message = "허용 최대 입찰 금액은 필수입니다")
        @Positive(message = "허용 최대 입찰 금액은 0보다 커야 합니다")
        Long maxAcceptableAmount
) {
}
