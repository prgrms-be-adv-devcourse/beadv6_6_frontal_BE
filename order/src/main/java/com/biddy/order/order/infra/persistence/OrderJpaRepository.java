package com.biddy.order.order.infra.persistence;

import com.biddy.order.order.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
}
