package com.biddy.auction.common.exception;

import lombok.Getter;

/**
 * 비즈니스 로직 예외.
 *
 * <p>도메인 규칙 위반 시 발생하며, {@code ErrorCode}를 통해
 * HTTP 상태 코드와 에러 메시지를 일관되게 전달한다.</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    }
}
