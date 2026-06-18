package com.biddy.payment.global.response;

public record ErrorResponse(
        boolean success,
        String code,
        String message
) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(false, code, message);
    }
}
