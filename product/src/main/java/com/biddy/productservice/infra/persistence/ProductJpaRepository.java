package com.biddy.productservice.infra.persistence;

import com.biddy.productservice.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import com.biddy.productservice.domain.model.SaleType;
import java.util.List;


public interface ProductJpaRepository extends JpaRepository<Product, Long> {
    List<Product> findBySellerId(Long sellerId);

    List<Product> findBySaleType(SaleType saleType);
}
