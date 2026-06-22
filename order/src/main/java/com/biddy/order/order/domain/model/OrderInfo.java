package com.biddy.order.order.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "order_price", nullable = false)
    private Long orderPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity; // 수량 추가

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public OrderInfo(Order order, Long orderPrice, Integer quantity, UUID productId, Long sellerId) {
        this.order = order;
        this.orderPrice = orderPrice;
        this.quantity = quantity;
        this.productId = productId;
        this.sellerId = sellerId;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
