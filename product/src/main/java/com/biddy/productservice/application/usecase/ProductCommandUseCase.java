package com.biddy.productservice.application.usecase;

import com.biddy.productservice.domain.model.Product;
import com.biddy.productservice.presentation.dto.ProductCreateRequest;
import com.biddy.productservice.presentation.dto.ProductUpdateRequest;

public interface ProductCommandUseCase {

    Product create(ProductCreateRequest request, Long memberId);
    Product update(Long id, ProductUpdateRequest request, Long memberId);
    void delete(Long id, Long memberId);
}
