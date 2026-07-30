package com.biddy.auction.common.exception;

import java.time.LocalDateTime;

/** v2 입찰 충돌 응답. 최신 snapshot을 받아 새 요청 여부를 사용자가 결정한다. */
public record BidConflictResponse(
        LocalDateTime timestamp,
        int status,
        String code,
        String message,
        long sequence,
        Long currentBid,
        Long nextMinimumBid,
        boolean retryable
) {
    public static BidConflictResponse of(BidConflictException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return new BidConflictResponse(
                LocalDateTime.now(),
                errorCode.getStatus().value(),
                errorCode.getCode(),
                exception.getMessage(),
                exception.getSequence(),
                exception.getCurrentBid(),
                exception.getNextMinimumBid(),
                exception.isRetryable()
        );
    }
}
