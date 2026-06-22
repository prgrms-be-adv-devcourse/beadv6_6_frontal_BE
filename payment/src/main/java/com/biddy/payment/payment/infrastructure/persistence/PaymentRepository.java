package com.biddy.payment.payment.infrastructure.persistence;

import com.biddy.payment.payment.domain.Payment;
import com.biddy.payment.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
