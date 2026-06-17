package com.biddy.order.cart.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "\"Cart\"", schema = "public")
@Getter
@Comment("장바구니 엔티티")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 1부터 자동 증가
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // 단순 Long 타입으로 매핑

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Cart(Long userId, UUID productId) {
        this.userId = userId;
        this.productId = productId;
    }

    // 데이터 저장 전 자동으로 생성일 입력
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
