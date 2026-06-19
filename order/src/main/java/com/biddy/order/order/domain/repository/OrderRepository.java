package com.biddy.order.order.domain.repository;

import com.biddy.order.order.domain.model.Order;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    List<Order> findByUserId(Long userId);
    Optional<Order> findById(Long id);
    Order save(Order order);
}
