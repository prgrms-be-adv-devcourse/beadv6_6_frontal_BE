package com.biddy.productservice.infra.persistence;

import com.biddy.productservice.domain.model.ProcessedOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedOrderJpaRepository extends JpaRepository<ProcessedOrder, Long> {
    boolean existsByOrderId(Long orderId);
}
