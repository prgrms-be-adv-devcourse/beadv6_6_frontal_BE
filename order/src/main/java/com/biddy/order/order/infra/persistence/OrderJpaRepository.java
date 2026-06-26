package com.biddy.order.order.infra.persistence;

import com.biddy.order.order.domain.model.Order;
import com.biddy.order.order.domain.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    
    // Batch Job 용도 쿼리 메소드 정의
    List<Order> findByStatusInAndCreatedAtBefore(List<OrderStatus> statuses, LocalDateTime threshold);
    List<Order> findByStatusAndUpdatedAtBefore(OrderStatus status, LocalDateTime threshold);
}
