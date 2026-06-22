package com.biddy.productservice.infra.persistence;

import com.biddy.productservice.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface ProductJpaRepository extends JpaRepository<Product, UUID> {
}
