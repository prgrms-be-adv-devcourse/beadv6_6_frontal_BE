package com.biddy.order.order.application.service;

import com.biddy.order.order.application.dto.OrderResult;
import com.biddy.order.order.application.usecase.OrderUseCase;
import com.biddy.order.order.domain.model.OrderStatus;
import com.biddy.order.order.domain.repository.OrderRepository;
import com.biddy.order.order.presentation.dto.request.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderApplicationService implements OrderUseCase {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderResult createOrder(Long userId, CreateOrderRequest request) {
        return null; // TODO: Implement order creation
    }

    @Override
    public List<OrderResult> getOrderList(Long userId) {
        return List.of(); // TODO: Implement order list query
    }

    @Override
    public OrderResult getOrderDetail(Long userId, Long orderId) {
        return null; // TODO: Implement order detail query
    }

    @Override
    @Transactional
    public OrderResult changeStatus(Long userId, Long orderId, OrderStatus status) {
        return null; // TODO: Implement order status change
    }

    @Override
    @Transactional
    public OrderResult cancelOrder(Long userId, Long orderId) {
        return null; // TODO: Implement order cancellation
    }

    @Override
    @Transactional
    public OrderResult completeOrder(Long userId, Long orderId) {
        return null; // TODO: Implement order completion
    }
}
