package com.biddy.productservice.domain.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "product_like",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "member_id"}))
public class ProductLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ProductLike() {}

    public ProductLike(Long productId, Long memberId) {
        this.productId = productId;
        this.memberId = memberId;
        this.createdAt = LocalDateTime.now();
    }
}
