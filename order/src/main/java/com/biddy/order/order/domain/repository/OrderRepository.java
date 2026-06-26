package com.biddy.order.order.domain.repository;

import com.biddy.order.order.domain.model.Order;
import com.biddy.order.order.domain.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    List<Order> findByUserId(Long userId);
    Optional<Order> findById(Long id);
    Order save(Order order);
    
    // Batch Job 용도 쿼리 메소드 정의
    List<Order> findByStatusInAndCreatedAtBefore(List<OrderStatus> statuses, LocalDateTime threshold);
    List<Order> findByStatusAndUpdatedAtBefore(OrderStatus status, LocalDateTime threshold);
}
