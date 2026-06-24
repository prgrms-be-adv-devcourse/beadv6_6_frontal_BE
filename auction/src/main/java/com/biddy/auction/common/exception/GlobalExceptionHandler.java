package com.biddy.auction.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 전역 예외 처리 핸들러.
 *
 * <p>{@code @RestControllerAdvice}로 모든 Controller의 예외를
 * 가로채어 일관된 {@code ErrorResponse} 형식으로 반환한다.</p>
 *
 * <p>처리 우선순위:</p>
 * <ol>
 *   <li>BusinessException — 비즈니스 규칙 위반 (4xx)</li>
 *   <li>MethodArgumentTypeMismatchException — 잘못된 파라미터 타입 (400)</li>
 *   <li>Exception — 예상치 못한 서버 오류 (500)</li>
 * </ol>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리.
     * ErrorCode에 정의된 HTTP 상태 코드와 메시지를 그대로 반환한다.
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        ErrorResponse response = ErrorResponse.of(e.getErrorCode(), e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(response);
    }

    /**
     * 파라미터 타입 불일치 처리.
     * 예: status=INVALID 같은 잘못된 Enum 값 전달 시 발생.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("TypeMismatch: parameter={}, value={}", e.getName(), e.getValue());
        String message = String.format("'%s' 파라미터 값 '%s'이(가) 유효하지 않습니다", e.getName(), e.getValue());
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT, message);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 예상치 못한 서버 오류 처리.
     * 에러 로그를 남기고 클라이언트에는 상세 정보를 노출하지 않는다.
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled exception", e);
        ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_ERROR);
        return ResponseEntity.internalServerError().body(response);
    }
}
