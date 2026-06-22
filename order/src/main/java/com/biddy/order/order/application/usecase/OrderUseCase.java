package com.biddy.order.order.application.usecase;

import com.biddy.order.order.application.dto.OrderPaymentInfoResult;
import com.biddy.order.order.application.dto.OrderResult;
import com.biddy.order.order.domain.model.OrderStatus;
import com.biddy.order.order.presentation.dto.request.CreateOrderRequest;
import java.util.List;

public interface OrderUseCase {
    OrderResult createOrder(Long userId, CreateOrderRequest request);
    List<OrderResult> getOrderList(Long userId);
    OrderResult getOrderDetail(Long userId, Long orderId);
    
    // 추가된 상태 변경 유스케이스들
    OrderResult changeStatus(Long userId, Long orderId, OrderStatus status);
    OrderResult cancelOrder(Long userId, Long orderId);
    OrderResult completeOrder(Long userId, Long orderId);
    OrderResult startPaymentProcessing(Long userId, Long orderId);

    OrderPaymentInfoResult getPaymentInfo(Long orderId);
}
