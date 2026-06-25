package com.biddy.productservice.domain.repository;

import com.biddy.productservice.domain.model.Product;
import com.biddy.productservice.domain.model.SaleType;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    List<Product> findAll();

    List<Product> findAllVisible();

    void delete(Product product);

    List<Product> findBySellerId(Long sellerId);

    List<Product> saveAll(List<Product> products);

    List<Product> findBySaleType(SaleType saleType);

    List<Product> findBySaleTypeVisible(SaleType saleType);

    List<Product> findAllById(List<Long> ids);
}
