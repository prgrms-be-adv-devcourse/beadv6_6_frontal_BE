package com.biddy.payment.wallet.infrastructure.persistence;

import com.biddy.payment.wallet.domain.DepositTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositTransactionRepository extends JpaRepository<DepositTransaction, Long> {

    List<DepositTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
