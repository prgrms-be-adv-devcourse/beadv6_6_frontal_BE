package com.biddy.productservice.domain.repository;

import com.biddy.productservice.domain.model.Product;
import com.biddy.productservice.domain.model.SaleType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(UUID id);

    List<Product> findAll();

    void delete(Product product);

    List<Product> findBySellerId(Long sellerId);

    List<Product> saveAll(List<Product> products);

    List<Product> findBySaleType(SaleType saleType);

    List<Product> findAllById(List<UUID> ids);
}
