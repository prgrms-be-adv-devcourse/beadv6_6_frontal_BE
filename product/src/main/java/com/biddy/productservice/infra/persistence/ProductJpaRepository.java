package com.biddy.productservice.infra.persistence;

import com.biddy.productservice.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import com.biddy.productservice.domain.model.SaleType;
import java.util.List;

import java.util.UUID;


public interface ProductJpaRepository extends JpaRepository<Product, UUID> {
    List<Product> findBySellerId(UUID sellerId);

    List<Product> findBySaleType(SaleType saleType);
}
