package com.biddy.auction.bid.application.dto;

import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

/** 서버 계산 입찰 명령. observedSequence가 null이면 v1 호환 요청이다. */
public record PlaceBidCommand(
        String auctionId,
        Long bidderId,
        UUID requestId,
        Long observedSequence,
        Long maxAcceptableAmount
) {
    /** 기존 v1 amount 계약을 정식 서버 계산 명령으로 변환한다. */
    public static PlaceBidCommand compatibleV1(String auctionId, Long bidderId, Long amount) {
        String idempotencySource = "v1:" + auctionId + ":" + bidderId + ":" + amount;
        return new PlaceBidCommand(
                auctionId,
                bidderId,
                UUID.nameUUIDFromBytes(idempotencySource.getBytes(UTF_8)),
                null,
                amount
        );
    }
}
