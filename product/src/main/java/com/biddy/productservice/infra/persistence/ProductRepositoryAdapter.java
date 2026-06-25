package com.biddy.productservice.infra.persistence;

import com.biddy.productservice.domain.model.Product;
import com.biddy.productservice.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import com.biddy.productservice.domain.model.SaleType;

import java.util.List;
import java.util.Optional;


@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Product save(Product product){
        return productJpaRepository.save(product);
    }
    @Override
    public Optional<Product> findById(Long id){
        return productJpaRepository.findById(id);
    }
    @Override
    public List<Product> findAll(){
        return productJpaRepository.findAll();
    }

    @Override
    public List<Product> findAllVisible(){
        return productJpaRepository.findAllVisible();
    }
    @Override
    public void delete(Product product){
        productJpaRepository.delete(product);
    }

    @Override
    public List<Product> findBySellerId(Long sellerId){
        return productJpaRepository.findBySellerId(sellerId);
    }

    @Override
    public List<Product> saveAll(List<Product> products){
        return productJpaRepository.saveAll(products);
    }

    @Override
    public List<Product> findBySaleType(SaleType saleType){
        return productJpaRepository.findBySaleType(saleType);
    }

    @Override
    public List<Product> findBySaleTypeVisible(SaleType saleType){
        return productJpaRepository.findBySaleTypeVisible(saleType);
    }

    @Override
    public List<Product> findAllById(List<Long> ids) { return productJpaRepository.findAllById(ids); }
}
