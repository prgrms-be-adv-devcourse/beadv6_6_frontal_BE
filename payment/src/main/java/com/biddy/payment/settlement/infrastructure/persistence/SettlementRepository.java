package com.biddy.payment.settlement.infrastructure.persistence;

import com.biddy.payment.settlement.domain.Settlement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Settlement> findAllByOrderByCreatedAtDesc();

    boolean existsByOrderId(Long orderId);

    Optional<Settlement> findByOrderId(Long orderId);
}
