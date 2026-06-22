package com.biddy.payment.payment.infrastructure.persistence;

import com.biddy.payment.payment.domain.Payment;
import com.biddy.payment.payment.domain.PaymentStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);

    Optional<Payment> findFirstByOrderIdOrderByIdDesc(Long orderId);
}
