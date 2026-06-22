package com.biddy.productservice.infra.persistence;

import com.biddy.productservice.domain.model.Product;
import com.biddy.productservice.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Product save(Product product){
        return productJpaRepository.save(product);
    }
    @Override
    public Optional<Product> findById(UUID id){
        return productJpaRepository.findById(id);
    }
    @Override
    public List<Product> findAll(){
        return productJpaRepository.findAll();
    }
    @Override
    public void delete(Product product){
        productJpaRepository.delete(product);
    }
}
