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
    
    Optional<Order> findByAuctionId(String auctionId);
    List<Order> findExpiredOrders(List<OrderStatus> statuses, LocalDateTime normalThreshold, LocalDateTime now);
    
    // Batch Job 용도 쿼리 메소드 정의
    List<Order> findByStatusInAndCreatedAtBefore(List<OrderStatus> statuses, LocalDateTime threshold);
    List<Order> findByStatusAndUpdatedAtBefore(OrderStatus status, LocalDateTime threshold);
}
