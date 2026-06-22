package com.biddy.order.order.infra.persistence;

import com.biddy.order.order.domain.model.OrderInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderInfoJpaRepository extends JpaRepository<OrderInfo, Long> {
}
