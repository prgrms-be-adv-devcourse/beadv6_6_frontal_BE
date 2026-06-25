package com.biddy.payment.payment.infrastructure.client.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OrderPaymentStatusTest {

    @Test
    void orderStatus를PaymentStatus로변환한다() {
        assertThat(OrderPaymentStatus.from("PENDING"))
                .isEqualTo(OrderPaymentStatus.PAYMENT_PENDING);
        assertThat(OrderPaymentStatus.from("PROCESSING"))
                .isEqualTo(OrderPaymentStatus.PAYMENT_PROCESSING);
        assertThat(OrderPaymentStatus.from("COMPLETED"))
                .isEqualTo(OrderPaymentStatus.PAID);
        assertThat(OrderPaymentStatus.from("CANCELLED"))
                .isEqualTo(OrderPaymentStatus.CANCELLED);
    }

    @Test
    void 알수없는주문상태는거부한다() {
        assertThatThrownBy(() -> OrderPaymentStatus.from("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 주문 상태");
    }
}
