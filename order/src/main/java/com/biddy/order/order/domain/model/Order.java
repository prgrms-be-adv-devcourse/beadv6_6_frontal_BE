package com.biddy.order.order.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "\"order\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice; // BigDecimal -> Long 변경

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderInfo> orderInfos = new ArrayList<>(); // OrderItem -> OrderInfo 변경

    @Builder
    public Order(Long userId, OrderStatus status, Long totalPrice, List<OrderInfo> orderInfos) {
        this.userId = userId;
        this.status = status;
        this.totalPrice = totalPrice;
        if (orderInfos != null) {
            this.orderInfos = orderInfos;
        }
    }

    // 주문 상태 변경 메소드
    public void updateStatus(OrderStatus status) {
        if (this.status == OrderStatus.COMPLETED || this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 완료되거나 취소된 주문은 상태를 변경할 수 없습니다.");
        }
        if (status == OrderStatus.COMPLETED && this.status != OrderStatus.PAID) {
            throw new IllegalStateException("주문 완료(구매확정) 상태는 결제 완료(PAID) 상태에서만 전이될 수 있습니다.");
        }
        this.status = status;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
