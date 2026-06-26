package com.biddy.productservice.infra.persistence;

import com.biddy.productservice.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.biddy.productservice.domain.model.SaleType;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface ProductJpaRepository extends JpaRepository<Product, Long> {
    List<Product> findBySellerId(Long sellerId);

    List<Product> findBySaleType(SaleType saleType);

    @Query("SELECT p FROM Product p WHERE p.status NOT IN ('SOLD_OUT', 'HIDDEN')")
    List<Product> findAllVisible();

    @Query("SELECT p FROM Product p WHERE p.saleType = :saleType AND p.status NOT IN ('SOLD_OUT', 'HIDDEN')")
    List<Product> findBySaleTypeVisible(@Param("saleType") SaleType saleType);
}
