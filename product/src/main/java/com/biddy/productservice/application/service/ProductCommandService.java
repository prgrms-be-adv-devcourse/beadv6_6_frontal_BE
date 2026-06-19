package com.biddy.productservice.application.service;

import com.biddy.productservice.application.usecase.ProductCommandUseCase;
import com.biddy.productservice.domain.model.Product;
import com.biddy.productservice.domain.repository.ProductRepository;
import com.biddy.productservice.presentation.dto.ProductCreateRequest;
import com.biddy.productservice.presentation.dto.ProductUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional
public class ProductCommandService implements ProductCommandUseCase {

    private final ProductRepository productRepository;

    // 외부 acl 제외, event는 추후 추가 예정
    @Override
    public Product create(ProductCreateRequest request){
        Product product = Product.create(request.sellerId(),request.name(),request.description(),
                request.price(),request.stock(),request.status(),request.category(),
                request.saleType(),request.brand(),request.creatorId());

        Product savedProduct = productRepository.save(product);
        return savedProduct;

    }

    @Override
    public Product update(UUID id, ProductUpdateRequest request){
        Product product = productRepository.findById(id)
                .orElseThrow( () -> new ResponseStatusException(HttpStatus.NOT_FOUND,"상품을 찾을 수 없습니다."));

        product.update(request.name(),request.description(),request.price(),request.stock(),
                request.status(),request.category(),request.brand(),request.modifierId());
        return productRepository.save(product);
    }
    @Override
    public void delete (UUID id){
        Product product = productRepository.findById(id)
                .orElseThrow( () -> new ResponseStatusException(HttpStatus.NOT_FOUND,"상품을 찾을 수 없습니다."));

        productRepository.delete(product);
    }
}
