package com.biddy.payment.payment.infrastructure.persistence;

import com.biddy.payment.payment.domain.PaymentCancel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentCancelRepository extends JpaRepository<PaymentCancel, Long> {

    List<PaymentCancel> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    @Query("select coalesce(sum(c.cancelAmount), 0) from PaymentCancel c where c.paymentId = :paymentId")
    Long sumCancelAmountByPaymentId(@Param("paymentId") Long paymentId);
}
