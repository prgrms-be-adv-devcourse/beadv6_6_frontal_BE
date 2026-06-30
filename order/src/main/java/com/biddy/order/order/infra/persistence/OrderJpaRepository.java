package com.biddy.order.order.infra.persistence;

import com.biddy.order.order.domain.model.Order;
import com.biddy.order.order.domain.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    
    Optional<Order> findByAuctionId(String auctionId);
    
    @Query("SELECT o FROM Order o " +
           "WHERE o.status IN :statuses " +
           "AND ((o.orderType = com.biddy.order.order.domain.model.OrderType.NORMAL AND o.createdAt < :normalThreshold) " +
           "OR (o.orderType = com.biddy.order.order.domain.model.OrderType.AUCTION AND o.paymentDeadline < :now))")
    List<Order> findExpiredOrders(@Param("statuses") List<OrderStatus> statuses,
                                  @Param("normalThreshold") LocalDateTime normalThreshold,
                                  @Param("now") LocalDateTime now);
    
    // Batch Job 용도 쿼리 메소드 정의
    List<Order> findByStatusInAndCreatedAtBefore(List<OrderStatus> statuses, LocalDateTime threshold);
    List<Order> findByStatusAndUpdatedAtBefore(OrderStatus status, LocalDateTime threshold);
}
