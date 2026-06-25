package com.biddy.order.order.presentation.controller;

import com.biddy.order.order.application.dto.OrderResult;
import com.biddy.order.order.application.usecase.OrderUseCase;
import com.biddy.order.order.domain.model.OrderStatus;
import com.biddy.order.order.presentation.dto.request.CreateOrderRequest;
import com.biddy.order.order.presentation.dto.response.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("${api.init}/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderUseCase orderUseCase;

    // 1. 주문 생성 (POST /order/create)
    @PostMapping("/create")
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("X-Member-Id") Long userId,
            @RequestBody CreateOrderRequest request
    ) {
        OrderResult result = orderUseCase.createOrder(userId, request);
        return ResponseEntity.ok(OrderResponse.from(result));
    }

    // 2. 주문 목록 조회 (GET /order/list)
    @GetMapping("/list")
    public ResponseEntity<List<OrderResponse>> getOrderList(
            @RequestHeader("X-Member-Id") Long userId
    ) {
        List<OrderResponse> response = orderUseCase.getOrderList(userId).stream()
                .map(OrderResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    // 3. 주문 상세 조회 (GET /order/info)
    @GetMapping("/info")
    public ResponseEntity<OrderResponse> getOrderDetail(
            @RequestHeader("X-Member-Id") Long userId,
            @RequestParam("orderId") Long orderId
    ) {
        OrderResult result = orderUseCase.getOrderDetail(userId, orderId);
        return ResponseEntity.ok(OrderResponse.from(result));
    }

    // 4. 주문 상태 변경 (PUT /order/statusChange)
    @PutMapping("/statusChange")
    public ResponseEntity<OrderResponse> changeStatus(
            @RequestHeader("X-Member-Id") Long userId,
            @RequestParam("orderId") Long orderId,
            @RequestParam("status") OrderStatus status
    ) {
        OrderResult result = orderUseCase.changeStatus(userId, orderId, status);
        return ResponseEntity.ok(OrderResponse.from(result));
    }

    // 5. 주문 취소 (PUT /order/cancel)
    @PutMapping("/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @RequestHeader("X-Member-Id") Long userId,
            @RequestParam("orderId") Long orderId
    ) {
        OrderResult result = orderUseCase.cancelOrder(userId, orderId);
        return ResponseEntity.ok(OrderResponse.from(result));
    }

    // 6. 주문 완료 (PUT /order/complete)
    @PutMapping("/complete")
    public ResponseEntity<OrderResponse> completeOrder(
            @RequestHeader("X-Member-Id") Long userId,
            @RequestParam("orderId") Long orderId
    ) {
        OrderResult result = orderUseCase.completeOrder(userId, orderId);
        return ResponseEntity.ok(OrderResponse.from(result));
    }
}
