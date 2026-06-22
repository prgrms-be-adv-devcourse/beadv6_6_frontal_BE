package com.biddy.auction.common.exception;

import java.time.LocalDateTime;

/**
 * 에러 응답 DTO.
 *
 * <p>클라이언트에게 일관된 에러 형식을 제공한다.</p>
 *
 * @param timestamp 에러 발생 시각
 * @param status HTTP 상태 코드
 * @param code 비즈니스 에러 코드
 * @param message 에러 메시지
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String code,
        String message
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
                LocalDateTime.now(),
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage()
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(
                LocalDateTime.now(),
                errorCode.getStatus().value(),
                errorCode.getCode(),
                message
        );
    }
}
