package com.biddy.order.order.infra.persistence;

import com.biddy.order.order.domain.model.Order;
import com.biddy.order.order.domain.model.OrderStatus;
import com.biddy.order.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public List<Order> findByUserId(Long userId) {
        return orderJpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderJpaRepository.findById(id);
    }

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<Order> findByAuctionId(String auctionId) {
        return orderJpaRepository.findByAuctionId(auctionId);
    }

    @Override
    public List<Order> findExpiredOrders(List<OrderStatus> statuses, LocalDateTime normalThreshold, LocalDateTime now) {
        return orderJpaRepository.findExpiredOrders(statuses, normalThreshold, now);
    }

    @Override
    public List<Order> findByStatusInAndCreatedAtBefore(List<OrderStatus> statuses, LocalDateTime threshold) {
        return orderJpaRepository.findByStatusInAndCreatedAtBefore(statuses, threshold);
    }

    @Override
    public List<Order> findByStatusAndUpdatedAtBefore(OrderStatus status, LocalDateTime threshold) {
        return orderJpaRepository.findByStatusAndUpdatedAtBefore(status, threshold);
    }
}
