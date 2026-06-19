package com.biddy.productservice.domain.model;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(name = "\"product\"", schema = "public")
@Schema(description = "상품 정보")
public class Product {

    @Id
    @Schema(description = "상품 ID", example = "11111111-1111-1111-1111-111111111111", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @Column(name = "\"name\"",nullable = false,length = 255)
    @Schema(description = "상품명", example = "홍길동 판매소")
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    @Schema(description = "상품설명",example = "2026 한정판 굿즈")
    private String description;

    @Column(name = "price",nullable = false)
    @Schema(description = "상품가격",example = "50000")
    private BigDecimal price;

    @Column(name = "stock",nullable = false)
    @Schema(description = "상품재고",example = "10")
    private int stock;

    @Column(nullable = false,length = 20)
    @Schema(description = "상품상태",example = "ACTIVE")
    private String status;

    @Column(name = "category", nullable = false,length = 50)
    @Schema(description = "상품 카테고리",example = "한정판 굿즈")
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type",nullable = false,length = 20)
    @Schema(description = "판매 유형",example = "NORMAL")
    private SaleType saleType;

    @Column(name = "brand",length = 100)
    @Schema(description = "브랜드",example = "나이키")
    private String brand;

    @Column(name = "reg_dt",nullable = false)
    @Schema(description = "등록일시",example = "2026-06-15T18:10:00",accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime regDt;

    @Column(name = "modify_id", nullable = false)
    @Schema(description = "수정자 ID", example = "33333333-3333-3333-3333-333333333333")
    private UUID modifyId;

    @Column(name = "modify_dt",nullable = false)
    @Schema(description = "수정일시",example = "2026-06-16T18:30:00",accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime modifyDt;

    @Column(name = "seller_id",nullable = false)
    @Schema(description = "판매자 ID", example = "33333333-3333-3333-3333-333333333333")
    private UUID sellerId;

    @Column(name = "reg_id",nullable = false)
    @Schema(description = "등록자 ID", example = "33333333-3333-3333-3333-333333333333")
    private UUID regId;

    protected Product(){}

    private Product(UUID id, UUID sellerId, String name, String description, BigDecimal price, int stock, String status, String category,
                    SaleType saleType, String brand) {
        this.id = id;
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.status = status;
        this.category = category;
        this.saleType = saleType;
        this.brand = brand;
    }

    // Id는 randomUUID로 내부 생성
    public static Product create(UUID sellerId, String name, String description, BigDecimal price,
                                 int stock, String status, String category,
                                 SaleType saleType, String brand, UUID creatorId)
    {
        Product product = new Product(UUID.randomUUID(),sellerId,name,description,price,
                stock,status,category,saleType,brand);
        product.regId = creatorId;
        product.modifyId = creatorId;
        return product;
    }

    public void update(String name, String description, BigDecimal price, int stock, String status,
                       String category, String brand,UUID modifyId){
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.status = status;
        this.category = category;
        this.brand = brand;
        this.modifyId = modifyId;
    }
    @PrePersist
    public void onCreate(){
        if (id == null) {id = UUID.randomUUID();}
        if (regDt == null) { regDt = LocalDateTime.now();}
        if (modifyDt == null) { modifyDt = regDt;}
        if (status == null) { status = "ACTIVE";}
    }

    @PreUpdate
    public void onUpdate() {
        modifyDt = LocalDateTime.now();
        if(modifyId ==null) { modifyId = id;}
    }
}








