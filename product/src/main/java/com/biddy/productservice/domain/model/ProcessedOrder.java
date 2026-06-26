package com.biddy.productservice.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "processed_order")
public class ProcessedOrder {

    @Id
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    protected ProcessedOrder() {}

    public ProcessedOrder(Long orderId) {
        this.orderId = orderId;
        this.processedAt = LocalDateTime.now();
    }

    public Long getOrderId() {
        return orderId;
    }
}
