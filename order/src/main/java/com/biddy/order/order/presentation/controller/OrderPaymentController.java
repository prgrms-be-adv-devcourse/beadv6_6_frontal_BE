package com.biddy.order.order.presentation.controller;

import com.biddy.order.order.application.dto.OrderPaymentInfoResult;
import com.biddy.order.order.application.dto.OrderResult;
import com.biddy.order.order.application.usecase.OrderUseCase;
import com.biddy.order.order.presentation.dto.response.OrderPaymentInfoResponse;
import com.biddy.order.order.presentation.dto.response.OrderPaymentProcessingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OrderPaymentController {

    private final OrderUseCase orderUseCase;

    @GetMapping({"/api/orders/{orderId}/payment-info"})
    public ResponseEntity<OrderPaymentInfoResponse> getPaymentInfo(
            @PathVariable("orderId") Long orderId
    ) {
        OrderPaymentInfoResult result = orderUseCase.getPaymentInfo(orderId);
        return ResponseEntity.ok(new OrderPaymentInfoResponse(
                result.orderId(),
                result.userId(),
                result.totalPrice(),
                result.status(),
                result.updatedAt()
        ));
    }

    @PatchMapping({"/api/orders/{orderId}/payment-processing"})
    public ResponseEntity<OrderPaymentProcessingResponse> startPaymentProcessing(
            @PathVariable("orderId") Long orderId,
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId
    ) {
        Long userId = paramUserId != null ? paramUserId : headerUserId;
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        OrderResult result = orderUseCase.startPaymentProcessing(userId, orderId);
        return ResponseEntity.ok(new OrderPaymentProcessingResponse(
                result.id(),
                result.status(),
                result.updatedAt()
        ));
    }
}
