package com.biddy.payment.settlement.infrastructure.persistence;

import com.biddy.payment.settlement.domain.SettlementItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementItemRepository extends JpaRepository<SettlementItem, Long> {

    List<SettlementItem> findBySettlementId(Long settlementId);
}
