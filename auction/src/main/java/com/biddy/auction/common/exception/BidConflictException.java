package com.biddy.auction.common.exception;

import lombok.Getter;

/** 클라이언트가 최신 가격과 sequence로 입찰 화면을 복구할 수 있는 v2 충돌 예외. */
@Getter
public class BidConflictException extends BusinessException {

    private final long sequence;
    private final Long currentBid;
    private final Long nextMinimumBid;
    private final boolean retryable;

    public BidConflictException(
            ErrorCode errorCode,
            long sequence,
            Long currentBid,
            Long nextMinimumBid
    ) {
        super(errorCode);
        this.sequence = sequence;
        this.currentBid = currentBid;
        this.nextMinimumBid = nextMinimumBid;
        this.retryable = true;
    }
}
