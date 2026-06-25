package com.biddy.productservice.application.service;

import com.biddy.productservice.application.usecase.ProductQueryUseCase;
import com.biddy.productservice.domain.model.Product;
import com.biddy.productservice.domain.model.SaleType;
import com.biddy.productservice.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductQueryService implements ProductQueryUseCase {

    private final ProductRepository productRepository;

    @Override
    public Product getById(Long id){
        return productRepository.findById(id)
                .orElseThrow( ()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"상품을 찾을 수 없습니다."));
    }
    @Override
    public List<Product> getAll(){
        return productRepository.findAll();
    }

    @Override
    public List<Product> getBySaleType(SaleType saleType){
        return productRepository.findBySaleType(saleType);
    }

    public List<Product> getProductsByIds(List<Long> ids){ return productRepository.findAllById(ids); }
}
