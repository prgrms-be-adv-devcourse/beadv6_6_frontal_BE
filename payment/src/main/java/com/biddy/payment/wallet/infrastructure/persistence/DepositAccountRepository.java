package com.biddy.payment.wallet.infrastructure.persistence;

import com.biddy.payment.wallet.domain.DepositAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface DepositAccountRepository extends JpaRepository<DepositAccount, Long> {

    Optional<DepositAccount> findByUserId(Long userId);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<DepositAccount> findWithLockByUserId(Long userId);
}
